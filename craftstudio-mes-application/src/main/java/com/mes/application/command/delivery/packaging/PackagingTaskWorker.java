package com.mes.application.command.delivery.packaging;

import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.order.enums.OrderStatus;
import org.bson.Document;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.model.BulkWriteOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

@Component
public class PackagingTaskWorker {
    private static final Logger log = LoggerFactory.getLogger(PackagingTaskWorker.class);
    private final PackagingTaskStore store;
    public PackagingTaskWorker(PackagingTaskStore store) { this.store = store; }

    @Scheduled(fixedDelayString = "${delivery.packaging.poll-ms:1000}")
    public void tick() {
        store.recover();
        for (int i = 0; i < 10; i++) {
            Document job = store.claim();
            if (job == null) return;
            long start = System.nanoTime();
            try {
                apply(job);
                store.done(job);
                log.info("打包状态任务完成 taskId={} orderId={} attempt={} elapsedMs={}", job.getString("_id"),
                        job.getString("orderId"), job.getInteger("attempts"), (System.nanoTime() - start) / 1_000_000.0);
            } catch (Exception ex) {
                store.retry(job, ex);
                log.error("打包状态任务失败 taskId={} orderId={} attempt={}", job.getString("_id"),
                        job.getString("orderId"), job.getInteger("attempts"), ex);
            }
        }
    }

    void apply(Document job) {
        if (!Boolean.TRUE.equals(job.getBoolean("quantitiesApplied"))) {
            applyQuantities(job);
            store.quantitiesApplied(job);
        }
        refreshCompletion(job);
    }

    /** Shared by the request path and crash recovery. Only selected pieces are written here. */
    public void applyQuantities(Document job) {
        List<Document> items = validatedItems(job);
        Document pkg = job.get("result", Document.class);
        if (pkg == null) throw new IllegalStateException("Task has no persisted package result");
        List<String> pieceIds = items.stream().map(d -> d.getString("pieceId")).toList();
        Map<String, Document> selected = new HashMap<>();
        for (Document piece : store.mongo().getCollection("productionPiece").find(new Document("productionPieceId", new Document("$in", pieceIds))
                .append("manufacturerId", job.getString("manufacturerId")).append("deleteAt", null)).projection(pieceProjection()))
            selected.put(piece.getString("productionPieceId"), piece);
        List<WriteModel<Document>> transfers = new ArrayList<>();
        for (Document item : items) {
            Document piece = selected.get(item.getString("pieceId"));
            if (piece == null || !Objects.equals(piece.get("orderItemId"), item.get("orderItemId")))
                throw new IllegalStateException("Production piece missing/deleted or moved: " + item.getString("pieceId"));
            if (piece.getList("packagingAppliedJobIds", String.class, List.of()).contains(job.getString("_id"))) continue;
            Integer required = item.getInteger("requiredPackedQuantity");
            Document patch = new Document(PackagingPatch.transfer(piece, item.getInteger("quantity"), required, pkg, TypesettingStatus.COMPLETED.getCode()));
            Document update = new Document("$set", patch.append("updateTime", new Date()).append("packagingRevision", nextRevision(piece)))
                    .append("$addToSet", new Document("packagingAppliedJobIds", job.getString("_id")));
            transfers.add(new UpdateOneModel<>(snapshot(piece).append("packagingAppliedJobIds", new Document("$ne", job.getString("_id"))), update));
        }
        write(job, "productionPiece", transfers);
    }

    private List<Document> validatedItems(Document job) {
        List<Document> items = job.getList("items", Document.class);
        if (items == null || items.isEmpty()) throw new IllegalStateException("Task has no validated items");
        return items;
    }

    private void refreshCompletion(Document job) {
        List<Document> items = validatedItems(job);
        List<String> itemIds = items.stream().map(d -> d.getString("orderItemId")).distinct().toList();
        Map<String, Document> orderItems = new HashMap<>();
        for (Document item : store.mongo().getCollection("orderItem").find(new Document("orderItemId", new Document("$in", itemIds))
                .append("deleteAt", null).append("orderId", job.getString("orderId")).append("manufacturerId", job.getString("manufacturerId")))
                .projection(new Document("orderItemId", 1).append("quantity", 1).append("status", 1).append("isUrgent", 1).append("updateTime", 1)))
            orderItems.put(item.getString("orderItemId"), item);
        // Always refresh from current data, including after an interrupted/partially successful previous attempt.
        Map<String, List<Document>> groups = new HashMap<>();
        for (Document piece : store.mongo().getCollection("productionPiece").find(new Document("orderItemId", new Document("$in", itemIds))
                .append("manufacturerId", job.getString("manufacturerId")).append("deleteAt", null)
                .append("isRedo", new Document("$ne", true))).projection(pieceProjection()))
            groups.computeIfAbsent(piece.getString("orderItemId"), ignored -> new ArrayList<>()).add(piece);
        List<WriteModel<Document>> pieceCompletion = new ArrayList<>(), itemCompletion = new ArrayList<>();
        for (String itemId : itemIds) {
            Document orderItem = orderItems.get(itemId);
            if (orderItem == null || orderItem.getInteger("quantity", 0) <= 0) continue;
            List<Document> nonRedo = groups.getOrDefault(itemId, List.of());
            if (nonRedo.isEmpty() || !nonRedo.stream().allMatch(d -> PackagingPatch.fullyPacked(d, orderItem.getInteger("quantity")))) continue;
            for (Document piece : nonRedo) {
                Document patch = new Document();
                PackagingPatch.put(patch, piece, "status", TypesettingStatus.COMPLETED.getCode());
                if (Boolean.TRUE.equals(piece.getBoolean("isUrgent"))) patch.put("isUrgent", false);
                if (!patch.isEmpty()) pieceCompletion.add(new UpdateOneModel<>(snapshot(piece),
                        new Document("$set", patch.append("updateTime", new Date()).append("packagingRevision", nextRevision(piece)))));
            }
            Document patch = new Document();
            PackagingPatch.put(patch, orderItem, "status", OrderStatus.PACKAGED.getCode());
            if (Boolean.TRUE.equals(orderItem.getBoolean("isUrgent"))) patch.put("isUrgent", false);
            if (!patch.isEmpty()) itemCompletion.add(new UpdateOneModel<>(
                    new Document("_id", orderItem.get("_id")).append("deleteAt", null)
                            .append("updateTime", orderItem.get("updateTime")).append("quantity", orderItem.get("quantity"))
                            .append("status", orderItem.get("status")).append("isUrgent", orderItem.get("isUrgent")),
                    new Document("$set", patch.append("updateTime", new Date()))));
        }
        write(job, "productionPiece", pieceCompletion);
        write(job, "orderItem", itemCompletion);
    }
    private void write(Document job, String collection, List<WriteModel<Document>> writes) {
        store.heartbeat(job);
        if (!writes.isEmpty() && store.mongo().getCollection(collection)
                .bulkWrite(writes, new BulkWriteOptions().ordered(false)).getMatchedCount() != writes.size())
            throw new IllegalStateException("Packaging snapshot changed; retry with fresh state");
    }
    private long nextRevision(Document piece) {
        return piece.get("packagingRevision") instanceof Number n ? n.longValue() + 1 : 1;
    }
    private Document pieceProjection() {
        return new Document("productionPieceId", 1).append("orderItemId", 1).append("quantity", 1).append("procedureFlow.nodes", 1)
                .append("packagingAppliedJobIds", 1).append("packagingRevision", 1).append("status", 1).append("isUrgent", 1)
                .append("deliveryPkgInfos", 1).append("updateTime", 1);
    }

    /** Compare the state we read; writes touch only changed packaging fields. No upsert/replace. */
    static Document snapshot(Document piece) {
        return new Document("_id", piece.get("_id")).append("deleteAt", null).append("packagingRevision", piece.get("packagingRevision"))
                .append("updateTime", piece.get("updateTime")).append("procedureFlow.nodes", PackagingPatch.nodes(piece))
                .append("deliveryPkgInfos", piece.get("deliveryPkgInfos")).append("status", piece.get("status"))
                .append("isUrgent", piece.get("isUrgent"));
    }
}

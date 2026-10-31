package com.mes.application.command.delivery.packaging;

import com.mes.application.dto.req.delivery.DeliveryPkgAddRequest;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import org.bson.Document;
import org.springframework.stereotype.Service;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import com.mes.domain.base.repository.ApiResponse;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.function.Supplier;

/** Durable request identity and conservative order-level quantity reservation. No production state is changed here. */
@Service
public class PackagingOperation {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PackagingOperation.class);
    private final PackagingTaskStore store;
    private final PackagingTaskWorker worker;
    private static final ThreadLocal<Document> ACTIVE = new ThreadLocal<>();
    public PackagingOperation(PackagingTaskStore store, PackagingTaskWorker worker) {
        this.store = store;
        this.worker = worker;
    }
    public static boolean active() { return ACTIVE.get() != null; }
    public static String pkgId() { return ACTIVE.get().getString("pkgId"); }
    public void risk() { if (active()) store.risk(ACTIVE.get().getString("_id")); }
    public List<ProductionPiece> pieces(Collection<String> ids) { return store.pieces(ids, ACTIVE.get().getString("manufacturerId")); }

    public DeliveryPkg execute(DeliveryPkgAddRequest request, Supplier<DeliveryPkg> action) {
        if (request == null || request.getPieces() == null || request.getPieces().isEmpty()
                || request.getPieces().stream().anyMatch(i -> i == null || i.getPiece() == null || i.getQuantity() == null || i.getQuantity() <= 0))
            throw bad("打包零件及数量不能为空");
        var first = request.getPieces().getFirst().getPiece();
        String manufacturer = request.getManufacturerMetaId(), orderId = first.getOrderId();
        if (manufacturer == null || manufacturer.isBlank() || orderId == null || orderId.isBlank()) throw bad("工厂和订单不能为空");
        if (request.getPieces().stream().anyMatch(i -> !Objects.equals(orderId, i.getPiece().getOrderId()))) throw bad("仅支持同一订单打包");
        Set<String> ids = new HashSet<>();
        for (var item : request.getPieces()) if (item.getPiece().getProductionPieceId() == null
                || !ids.add(item.getPiece().getProductionPieceId())) throw bad("生产件不能为空或重复");
        String fingerprint = fingerprint(request);
        String requestId = request.getRequestId();
        // Legacy clients get deterministic replay; an intentional identical second package needs a fresh requestId.
        if (requestId == null || requestId.isBlank()) requestId = "legacy-" + fingerprint;
        String id = digest(manufacturer + ":" + requestId), orderKey = digest(manufacturer + ":" + orderId);
        Document fresh = new Document("_id", id).append("fingerprint", fingerprint).append("requestId", requestId)
                .append("manufacturerId", manufacturer).append("orderId", orderId).append("orderKey", orderKey)
                .append("state", "PREPARING").append("pkgId", "DP" + UUID.randomUUID().toString().replace("-", ""))
                .append("createdAt", new Date()).append("updatedAt", new Date()).append("attempts", 0);
        Document job = store.create(fresh);
        if (!Objects.equals(fingerprint, job.getString("fingerprint"))) throw bad("requestId 已用于不同的打包内容");
        if (!Objects.equals(fresh.getString("pkgId"), job.getString("pkgId"))) {
            if (job.get("result") != null && (Boolean.TRUE.equals(job.getBoolean("quantitiesApplied"))
                    || "DONE".equals(job.getString("state")))) {
                log.info("打包请求幂等重放 taskId={} orderId={} state={}", id, orderId, job.getString("state"));
                return decorate(store.result(job), job);
            }
            throw bad("打包请求已受理，当前状态=" + job.getString("state") + "，taskId=" + id + "，请勿重复打单");
        }
        try { store.lock(orderKey, id); }
        catch (RuntimeException ex) { store.failPreparation(id); throw bad(ex.getMessage()); }
        log.info("打包任务受理 taskId={} orderId={} pkgId={}", id, orderId, job.getString("pkgId"));
        ACTIVE.set(job);
        try {
            DeliveryPkg result = action.get();
            Document saved = store.publish(id, result); // Durable result + owned lease; a crash can be recovered without reprinting.
            try {
                com.mes.application.command.delivery.DeliveryPkgTrace.run("同步更新打包零件",
                        () -> worker.applyQuantities(saved));
                store.ready(saved); // Only after every selected piece is updated may this request return success.
            } catch (RuntimeException | Error ex) {
                try { store.retry(saved, ex); } catch (RuntimeException failure) { ex.addSuppressed(failure); }
                throw ex;
            }
            job.put("state", "READY");
            job.put("quantitiesApplied", true);
            log.info("打包任务就绪 taskId={} orderId={} pkgId={}", id, orderId, result.getDeliveryPkgId());
            return decorate(result, job);
        } catch (RuntimeException | Error ex) {
            try { store.failPreparation(id); } catch (RuntimeException failure) { ex.addSuppressed(failure); }
            throw ex;
        } finally { ACTIVE.remove(); }
    }

    public void validated(List<ProductionPiece> pieces, Map<String, Integer> quantities) {
        Document job = ACTIVE.get();
        if (job == null) return;
        List<OrderItem> items = store.remarkItems(pieces.stream().map(ProductionPiece::getOrderItemId).distinct().toList(), job.getString("manufacturerId"), job.getString("orderId"));
        Map<String, OrderItem> byId = new HashMap<>();
        for (OrderItem item : items) byId.put(item.getOrderItemId(), item);
        List<Document> inputs = new ArrayList<>();
        Set<String> canonicalIds = new HashSet<>();
        for (ProductionPiece piece : pieces) {
            if (!canonicalIds.add(piece.getId())) throw bad("同一生产件不能重复提交");
            OrderItem item = byId.get(piece.getOrderItemId());
            if (!Objects.equals(job.getString("manufacturerId"), piece.getManufacturerId()) || item == null
                    || !Objects.equals(job.getString("orderId"), item.getOrderId())) throw bad("生产件不属于当前订单或工厂");
            if (piece.getProcedureFlow() == null || piece.getProcedureFlow().getNodes() == null
                    || piece.getProcedureFlow().getNodes().stream().noneMatch(n -> "已打包".equals(n.getNodeName()))) throw bad("生产件缺少已打包节点");
            inputs.add(new Document("pieceId", piece.getProductionPieceId()).append("orderItemId", piece.getOrderItemId())
                    .append("quantity", quantities.get(piece.getId())).append("requiredPackedQuantity", item.getQuantity()));
        }
        job.put("remarkItems", items); // Request-local projection, never persisted in task document.
        store.validated(job.getString("_id"), inputs);
    }
    @SuppressWarnings("unchecked")
    public static List<OrderItem> cachedRemarkItems() {
        return ACTIVE.get() == null ? null : (List<OrderItem>) ACTIVE.get().get("remarkItems");
    }
    public <T> T exclusive(String manufacturer, String orderId, Supplier<T> action) {
        if (manufacturer == null || manufacturer.isBlank() || orderId == null || orderId.isBlank()) throw bad("订单和工厂不能为空");
        String key = digest(manufacturer + ":" + orderId), token = "manual-" + UUID.randomUUID();
        store.create(new Document("_id", token).append("orderKey", key).append("manufacturerId", manufacturer)
                .append("orderId", orderId).append("state", "PREPARING").append("createdAt", new Date()).append("updatedAt", new Date()));
        try { store.lock(key, token); }
        catch (RuntimeException ex) { store.failPreparation(token); throw bad(ex.getMessage()); }
        try {
            T result = action.get();
            store.finishManual(token, key);
            return result;
        } catch (RuntimeException | Error ex) {
            store.reviewManual(token); // Could be a partially applied manual operation. Do not silently release its reservation.
            throw ex;
        }
    }
    public Document status(String taskId, String manufacturer) {
        Document job = store.find(taskId);
        if (job == null || !Objects.equals(manufacturer, job.getString("manufacturerId"))) throw bad("打包任务不存在");
        return new Document("taskId", taskId).append("state", job.get("state")).append("orderId", job.get("orderId"))
                .append("pkgId", job.get("pkgId")).append("quantitiesApplied", job.get("quantitiesApplied"))
                .append("attempts", job.get("attempts"))
                .append("completedAt", job.get("completedAt")).append("lastErrorType", job.get("lastErrorType")).append("updatedAt", job.get("updatedAt"));
    }
    public boolean reconcile(String taskId, String manufacturer, DeliveryPkg pkg) {
        Document job = store.find(taskId);
        if (job == null || !Objects.equals(manufacturer, job.getString("manufacturerId"))
                || !Objects.equals(manufacturer, pkg.getManufacturerMetaId()) || !Objects.equals(job.get("orderId"), pkg.getOrderId()))
            throw bad("任务与包裹的工厂或订单不一致");
        List<Document> items = job.getList("items", Document.class);
        if (items == null || items.isEmpty() || pkg.getDeliveryPkgItems() == null) throw bad("没有可恢复的生产件明细");
        Map<String, Integer> expected = new HashMap<>(), actual = new HashMap<>();
        for (Document item : items) expected.merge(item.getString("pieceId"), item.getInteger("quantity"), Integer::sum);
        for (var item : pkg.getDeliveryPkgItems()) {
            if (item.getProductionPieceId() == null || item.getProductionPieceId().size() != 1 || item.getQuantity() == null) throw bad("包裹明细不匹配");
            actual.merge(item.getProductionPieceId().getFirst(), item.getQuantity(), Integer::sum);
        }
        if (!expected.equals(actual)) throw bad("包裹明细与任务预占数量不一致");
        boolean reconciled = store.reconcile(taskId, manufacturer, pkg);
        log.info("打包任务人工关联 taskId={} manufacturerId={} pkgId={} success={}", taskId, manufacturer, pkg.getDeliveryPkgId(), reconciled);
        return reconciled;
    }
    public boolean closeReviewed(String taskId, String manufacturer, String note) {
        if (note == null || note.isBlank()) throw bad("必须填写核对及处理结果");
        boolean closed = store.closeReviewed(taskId, manufacturer, note);
        log.info("打包任务人工核对关闭 taskId={} manufacturerId={} success={}", taskId, manufacturer, closed);
        return closed;
    }
    public boolean retry(String taskId, String manufacturer) { return store.retryReview(taskId, manufacturer); }
    private DeliveryPkg decorate(DeliveryPkg pkg, Document job) {
        pkg.setPackagingTaskId(job.getString("_id")); pkg.setPackagingState(job.getString("state")); return pkg;
    }
    private static BusinessNotAllowException bad(String message) { return new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, message); }
    public static String fingerprint(DeliveryPkgAddRequest request) {
        List<String> pieces = request.getPieces().stream().map(i -> i.getPiece().getProductionPieceId() + ":" + i.getQuantity()).sorted().toList();
        var first = request.getPieces().getFirst().getPiece();
        Document value = new Document("manufacturer", request.getManufacturerMetaId()).append("order", first.getOrderId())
                .append("pieces", pieces).append("carrier", first.getLogisticsCarrierInfo() == null ? null : first.getLogisticsCarrierInfo().getCarrierId())
                .append("preset", first.getLogisticsCarrierInfo() == null ? null : first.getLogisticsCarrierInfo().getPresetType())
                .append("route", request.getRouteId()).append("routeNode", request.getRouteNodeId())
                .append("printer", request.getDeliverySiidId()).append("sender", request.getDeliveryManId()).append("siid", request.getSiid());
        return digest(value.toJson());
    }
    private static String digest(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}

package com.mes.application.command.delivery.packaging;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.infra.dal.manufacurer.ProductionPiece.po.ProductionPiecePo;
import com.mes.infra.dal.order.po.OrderItemPo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.annotation.PreDestroy;

@Repository
public class PackagingTaskStore {
    public static final String TASKS = "deliveryPackagingTask", LOCKS = "deliveryPackagingOrderLock";
    private static final long LOCK_LEASE_MS = 120_000;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PackagingTaskStore.class);
    private final ConcurrentMap<String, String> heldLocks = new ConcurrentHashMap<>();
    private final MongoTemplate mongo;
    public PackagingTaskStore(MongoTemplate mongo) { this.mongo = mongo; }
    private Query id(String id) { return Query.query(Criteria.where("_id").is(id)); }
    public Document find(String id) { return mongo.findOne(id(id), Document.class, TASKS); }
    public Document create(Document task) {
        try { return mongo.insert(task, TASKS); }
        catch (DuplicateKeyException duplicate) { return find(task.getString("_id")); }
    }

    /** An execution owns the lock; task state never extends its lifetime. */
    public String lock(String key, String taskId) {
        String owner = UUID.randomUUID().toString();
        Date now = new Date();
        // Also remove legacy locks without a lease, including old manual REVIEW reservations.
        mongo.remove(Query.query(Criteria.where("_id").is(key).orOperator(
                Criteria.where("leaseUntil").lte(now), Criteria.where("leaseUntil").exists(false))), LOCKS);
        try {
            mongo.insert(new Document("_id", key).append("taskId", taskId).append("owner", owner)
                    .append("createdAt", now).append("leaseUntil", new Date(now.getTime() + LOCK_LEASE_MS)), LOCKS);
            heldLocks.put(key, owner);
            return owner;
        } catch (DuplicateKeyException busy) {
            Document lock = mongo.findOne(id(key), Document.class, LOCKS);
            throw new IllegalStateException("该订单正在执行打包相关操作，taskId="
                    + (lock == null ? "unknown" : lock.getString("taskId")) + "，请稍后重试");
        }
    }
    private Query lockOwner(String key, String owner) {
        return Query.query(Criteria.where("_id").is(key).and("owner").is(owner));
    }
    public void release(String key, String owner) {
        if (owner == null) return;
        heldLocks.remove(key, owner);
        try { mongo.remove(lockOwner(key, owner), LOCKS); }
        catch (RuntimeException ex) { log.error("释放打包锁失败，等待租约过期 orderKey={}", key, ex); }
    }
    /** Existing terminal-task recovery only touches legacy reservations. */
    public void unlock(String key, String taskId) {
        mongo.remove(Query.query(Criteria.where("_id").is(key).and("taskId").is(taskId)
                .and("owner").exists(false)), LOCKS);
    }
    @Scheduled(fixedDelay = 20_000)
    public void renewExecutionLocks() {
        heldLocks.forEach((key, owner) -> {
            try {
                if (!renewExecutionLock(key, owner)) heldLocks.remove(key, owner);
            } catch (RuntimeException ex) { log.error("打包锁续租失败 orderKey={}", key, ex); }
        });
    }
    private boolean renewExecutionLock(String key, String owner) {
        Query query = lockOwner(key, owner);
        query.addCriteria(Criteria.where("leaseUntil").gt(new Date()));
        return mongo.updateFirst(query, new Update().set("leaseUntil",
                new Date(System.currentTimeMillis() + LOCK_LEASE_MS)), LOCKS).getMatchedCount() == 1;
    }
    @PreDestroy
    public void releaseProcessLocks() { heldLocks.forEach(this::release); }

    /** Config-side emergency operation: delete locks, preserving packages, quantities and task history. */
    public long clearAllLocks() {
        long deleted = mongo.remove(new Query(), LOCKS).getDeletedCount();
        log.warn("管理端清理全部打包锁 count={}", deleted);
        return deleted;
    }
    public void validated(String id, List<Document> items) {
        require(mongo.updateFirst(Query.query(Criteria.where("_id").is(id).and("state").is("PREPARING")),
                new Update().set("items", items).set("updatedAt", new Date()), TASKS).getMatchedCount());
    }
    public void risk(String id) {
        require(mongo.updateFirst(Query.query(Criteria.where("_id").is(id).and("state").is("PREPARING")),
                new Update().set("sideEffectPossible", true).set("updatedAt", new Date()), TASKS).getMatchedCount());
    }
    /** Persist the printing result before touching quantities, and hold a lease during synchronous visibility updates. */
    public Document publish(String id, DeliveryPkg pkg) {
        Document result = new Document(); mongo.getConverter().write(pkg, result);
        Document claimed = mongo.findAndModify(Query.query(Criteria.where("_id").is(id).and("state").is("PREPARING")),
                new Update().set("result", result).set("pkgId", pkg.getDeliveryPkgId()).set("state", "PROCESSING")
                        .set("owner", UUID.randomUUID().toString()).set("quantitiesApplied", false)
                        .set("leaseUntil", new Date(System.currentTimeMillis() + 120_000))
                        .set("updatedAt", new Date()).inc("attempts", 1),
                FindAndModifyOptions.options().returnNew(true), Document.class, TASKS);
        if (claimed == null) throw new IllegalStateException("Packaging task ownership changed");
        return claimed;
    }
    public void ready(Document job) {
        require(mongo.updateFirst(owned(job), new Update().set("quantitiesApplied", true).set("state", "READY")
                .set("nextAttemptAt", new Date()).set("updatedAt", new Date()).unset("owner").unset("leaseUntil"), TASKS).getMatchedCount());
    }
    public void quantitiesApplied(Document job) {
        require(mongo.updateFirst(owned(job), new Update().set("quantitiesApplied", true).set("updatedAt", new Date()), TASKS).getMatchedCount());
    }
    public DeliveryPkg result(Document task) {
        return mongo.getConverter().read(DeliveryPkg.class, task.get("result", Document.class));
    }
    public void failPreparation(String id) {
        Document task = find(id);
        if (task == null || !"PREPARING".equals(task.getString("state"))) return;
        boolean uncertain = Boolean.TRUE.equals(task.getBoolean("sideEffectPossible"));
        var changed = mongo.updateFirst(Query.query(Criteria.where("_id").is(id).and("state").is("PREPARING")),
                new Update().set("state", uncertain ? "REVIEW" : "FAILED").set("updatedAt", new Date()), TASKS);
        if (!uncertain && changed.getModifiedCount() == 1) unlock(task.getString("orderKey"), id);
    }
    public Document claim() {
        Date now = new Date();
        Criteria eligible = new Criteria().orOperator(
                new Criteria().andOperator(Criteria.where("state").in("READY", "RETRY"), Criteria.where("nextAttemptAt").lte(now)),
                new Criteria().andOperator(Criteria.where("state").is("PROCESSING"), Criteria.where("leaseUntil").lt(now)));
        return mongo.findAndModify(Query.query(eligible).with(Sort.by("createdAt")),
                new Update().set("state", "PROCESSING").set("owner", UUID.randomUUID().toString())
                        .set("leaseUntil", new Date(now.getTime() + 120_000)).set("updatedAt", now).inc("attempts", 1),
                FindAndModifyOptions.options().returnNew(true), Document.class, TASKS);
    }
    public void heartbeat(Document job) {
        String key = job.getString("orderKey"), owner = job.getString("executionLockOwner");
        if (owner == null || !Objects.equals(owner, heldLocks.get(key)) || !renewExecutionLock(key, owner))
            throw new IllegalStateException("Packaging execution lock lost");
        require(mongo.updateFirst(owned(job), new Update().set("leaseUntil", new Date(System.currentTimeMillis() + 120_000)), TASKS)
                .getMatchedCount());
    }
    private Query owned(Document job) {
        return Query.query(Criteria.where("_id").is(job.getString("_id")).and("state").is("PROCESSING")
                .and("owner").is(job.getString("owner")));
    }
    public void done(Document job) {
        require(mongo.updateFirst(owned(job), new Update().set("state", "DONE").set("completedAt", new Date()).set("updatedAt", new Date()), TASKS).getMatchedCount());
    }
    public void deferForBusyOrder(Document job) {
        require(mongo.updateFirst(owned(job), new Update().set("state", "RETRY").inc("attempts", -1)
                .set("nextAttemptAt", new Date(System.currentTimeMillis() + 1000)).set("updatedAt", new Date())
                .unset("owner").unset("leaseUntil"), TASKS).getMatchedCount());
    }
    public void retry(Document job, Throwable ex) {
        int attempts = job.getInteger("attempts", 1);
        mongo.updateFirst(owned(job), new Update().set("state", attempts >= 10 ? "REVIEW" : "RETRY")
                .set("lastErrorType", ex.getClass().getSimpleName()).set("updatedAt", new Date())
                .set("nextAttemptAt", new Date(System.currentTimeMillis() + Math.min(300_000L, 1000L << Math.min(attempts, 8)))), TASKS);
    }
    public void recover() {
        mongo.remove(Query.query(new Criteria().orOperator(Criteria.where("leaseUntil").lte(new Date()),
                Criteria.where("leaseUntil").exists(false))), LOCKS);
        // Preserve ambiguous results for reconciliation; execution locks expire independently.
        mongo.updateMulti(Query.query(Criteria.where("state").is("PREPARING").and("updatedAt")
                        .lt(new Date(System.currentTimeMillis() - 1_800_000))),
                new Update().set("state", "REVIEW").set("lastErrorType", "InterruptedPreparation"), TASKS);

    }
    public void finishManual(String taskId, String orderKey) {
        require(mongo.updateFirst(Query.query(Criteria.where("_id").is(taskId).and("state").is("PREPARING")),
                new Update().set("state", "DONE").set("updatedAt", new Date()), TASKS).getMatchedCount());
    }
    public void reviewManual(String taskId) {
        mongo.updateFirst(id(taskId), new Update().set("state", "REVIEW").set("updatedAt", new Date()), TASKS);
    }

    public boolean retryReview(String taskId, String manufacturer) {
        return mongo.updateFirst(Query.query(Criteria.where("_id").is(taskId).and("manufacturerId").is(manufacturer)
                        .and("state").is("REVIEW").and("result").ne(null)),
                new Update().set("state", "RETRY").set("attempts", 0).set("nextAttemptAt", new Date()), TASKS).getModifiedCount() == 1;
    }
    /** Clear review restrictions without deleting history or reprinting persisted packages. */
    public Document clearReviewLocks(String manufacturer, String orderId, String taskId, String note) {
        Date now = new Date();
        long closed = mongo.updateMulti(reviewScope(manufacturer, orderId, taskId, false),
                new Update().set("state", "CLOSED").set("reviewNote", note).set("updatedAt", now), TASKS).getModifiedCount();
        long retried = mongo.updateMulti(reviewScope(manufacturer, orderId, taskId, true),
                new Update().set("state", "RETRY").set("attempts", 0).set("nextAttemptAt", now)
                        .set("reviewNote", note).set("updatedAt", now), TASKS).getModifiedCount();
        return new Document("clearedCount", closed + retried).append("closedCount", closed).append("retryCount", retried);
    }
    private Query reviewScope(String manufacturer, String orderId, String taskId, boolean hasResult) {
        Criteria scope = Criteria.where("manufacturerId").is(manufacturer).and("state").is("REVIEW");
        if (orderId != null && !orderId.isBlank()) scope.and("orderId").is(orderId);
        if (taskId != null && !taskId.isBlank()) scope.and("_id").is(taskId);
        if (hasResult) scope.and("result").ne(null);
        else scope.and("result").is(null);
        return Query.query(scope);
    }
    public List<ProductionPiece> pieces(Collection<String> ids, String manufacturer) {
        Query query = Query.query(new Criteria().andOperator(Criteria.where("deleteAt").is(null), Criteria.where("manufacturerId").is(manufacturer),
                new Criteria().orOperator(Criteria.where("productionPieceId").in(ids), Criteria.where("_id").in(ids))));
        for (String field : List.of("productionPieceId", "manufacturerId", "orderItemId", "quantity", "isRedo", "status",
                "procedureFlow.nodes.nodeId", "procedureFlow.nodes.nodeName", "procedureFlow.nodes.nodeStatus",
                "procedureFlow.nodes.pieceQuantity")) query.fields().include(field);
        return mongo.find(query, ProductionPiecePo.class).stream().map(ProductionPiecePo::toDO).toList();
    }
    public List<OrderItem> remarkItems(Collection<String> ids, String manufacturer, String orderId) {
        if (ids.isEmpty()) return List.of();
        Query query = Query.query(Criteria.where("deleteAt").is(null).and("orderItemId").in(ids).and("manufacturerId").is(manufacturer).and("orderId").is(orderId));
        for (String field : List.of("orderItemId", "orderId", "manufacturerId", "quantity", "productionImgFile.name")) query.fields().include(field);
        return mongo.find(query, OrderItemPo.class).stream().map(OrderItemPo::toDO).toList();
    }
    public boolean reconcile(String taskId, String manufacturer, DeliveryPkg pkg) {
        Document result = new Document(); mongo.getConverter().write(pkg, result);
        return mongo.updateFirst(Query.query(Criteria.where("_id").is(taskId).and("manufacturerId").is(manufacturer)
                        .and("state").is("REVIEW").and("result").is(null).and("items.0").exists(true)),
                new Update().set("result", result).set("pkgId", pkg.getDeliveryPkgId()).set("state", "READY")
                        .set("nextAttemptAt", new Date()).set("updatedAt", new Date()), TASKS).getModifiedCount() == 1;
    }
    public boolean closeReviewed(String taskId, String manufacturer, String note) {
        Document job = find(taskId);
        if (job == null) return false;
        var result = mongo.updateFirst(Query.query(Criteria.where("_id").is(taskId).and("manufacturerId").is(manufacturer)
                        .and("state").is("REVIEW").and("result").is(null)),
                new Update().set("state", "CLOSED").set("reviewNote", note).set("updatedAt", new Date()), TASKS);
        if (result.getModifiedCount() == 1) unlock(job.getString("orderKey"), taskId);
        return result.getModifiedCount() == 1;
    }
    public MongoTemplate mongo() { return mongo; }
    private static void require(long count) { if (count != 1) throw new IllegalStateException("Packaging task ownership changed"); }
}


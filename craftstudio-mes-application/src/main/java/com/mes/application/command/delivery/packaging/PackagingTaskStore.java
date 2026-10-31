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

@Repository
public class PackagingTaskStore {
    public static final String TASKS = "deliveryPackagingTask", LOCKS = "deliveryPackagingOrderLock";
    private final MongoTemplate mongo;
    public PackagingTaskStore(MongoTemplate mongo) { this.mongo = mongo; }
    private Query id(String id) { return Query.query(Criteria.where("_id").is(id)); }
    public Document find(String id) { return mongo.findOne(id(id), Document.class, TASKS); }
    public Document create(Document task) {
        try { return mongo.insert(task, TASKS); }
        catch (DuplicateKeyException duplicate) { return find(task.getString("_id")); }
    }

    public void lock(String key, String taskId) {
        try { mongo.insert(new Document("_id", key).append("taskId", taskId).append("createdAt", new Date()), LOCKS); }
        catch (DuplicateKeyException busy) {
            Document lock = mongo.findOne(id(key), Document.class, LOCKS);
            throw new IllegalStateException("该订单存在未完成打包任务，taskId=" + (lock == null ? "unknown" : lock.getString("taskId")));
        }
    }
    public void unlock(String key, String taskId) {
        mongo.remove(Query.query(Criteria.where("_id").is(key).and("taskId").is(taskId)), LOCKS);
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
        require(mongo.updateFirst(owned(job), new Update().set("leaseUntil", new Date(System.currentTimeMillis() + 120_000)), TASKS)
                .getMatchedCount());
    }
    private Query owned(Document job) {
        return Query.query(Criteria.where("_id").is(job.getString("_id")).and("state").is("PROCESSING")
                .and("owner").is(job.getString("owner")));
    }
    public void done(Document job) {
        require(mongo.updateFirst(owned(job), new Update().set("state", "DONE").set("completedAt", new Date()).set("updatedAt", new Date()), TASKS).getMatchedCount());
        unlock(job.getString("orderKey"), job.getString("_id"));
    }
    public void retry(Document job, Throwable ex) {
        int attempts = job.getInteger("attempts", 1);
        mongo.updateFirst(owned(job), new Update().set("state", attempts >= 10 ? "REVIEW" : "RETRY")
                .set("lastErrorType", ex.getClass().getSimpleName()).set("updatedAt", new Date())
                .set("nextAttemptAt", new Date(System.currentTimeMillis() + Math.min(300_000L, 1000L << Math.min(attempts, 8)))), TASKS);
    }
    public void recover() {
        // Never reprint after a crash/ambiguous network result. Retain the order reservation for reconciliation.
        mongo.updateMulti(Query.query(Criteria.where("state").is("PREPARING").and("updatedAt")
                        .lt(new Date(System.currentTimeMillis() - 1_800_000))),
                new Update().set("state", "REVIEW").set("lastErrorType", "InterruptedPreparation"), TASKS);
        // A crash after DONE but before unlock must not permanently reserve the order.
        Query completed = Query.query(Criteria.where("state").in("DONE", "FAILED", "CLOSED").and("reservationReleased").ne(true)).limit(100);
        for (Document task : mongo.find(completed, Document.class, TASKS)) {
            unlock(task.getString("orderKey"), task.getString("_id"));
            mongo.updateFirst(id(task.getString("_id")), new Update().set("reservationReleased", true), TASKS);
        }
    }
    public void finishManual(String taskId, String orderKey) {
        require(mongo.updateFirst(Query.query(Criteria.where("_id").is(taskId).and("state").is("PREPARING")),
                new Update().set("state", "DONE").set("updatedAt", new Date()), TASKS).getMatchedCount());
        unlock(orderKey, taskId);
    }
    public void reviewManual(String taskId) {
        mongo.updateFirst(id(taskId), new Update().set("state", "REVIEW").set("updatedAt", new Date()), TASKS);
    }

    public boolean retryReview(String taskId, String manufacturer) {
        return mongo.updateFirst(Query.query(Criteria.where("_id").is(taskId).and("manufacturerId").is(manufacturer)
                        .and("state").is("REVIEW").and("result").ne(null)),
                new Update().set("state", "RETRY").set("attempts", 0).set("nextAttemptAt", new Date()), TASKS).getModifiedCount() == 1;
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

package com.mes.infra.dal.graphics;

import com.mes.domain.shared.graphics.GraphicsWorkflow;
import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.time.Duration;

@Repository
public class GraphicsWorkflowRepository {
    private static final String COLLECTION="graphics_workflow";
    private final MongoTemplate mongo;
    public GraphicsWorkflowRepository(MongoTemplate mongo){this.mongo=mongo;}
    @PostConstruct public void indexes(){
        mongo.indexOps(COLLECTION).ensureIndex(new Index().on("dedupKey",Sort.Direction.ASC).unique());
        mongo.indexOps(COLLECTION).ensureIndex(new Index().on("status",Sort.Direction.ASC).on("createdAt",Sort.Direction.ASC));
        mongo.indexOps(COLLECTION).ensureIndex(new Index().on("expireAt",Sort.Direction.ASC).expire(Duration.ZERO));
        mongo.indexOps(COLLECTION).ensureIndex(new Index().on("manufacturerMetaId",Sort.Direction.ASC).on("businessId",Sort.Direction.ASC));
    }
    public GraphicsWorkflow insert(String id,String tenant,String kind,String business,String payload,String dedup){
        Date now=new Date();
        Document doc=new Document("_id",id).append("manufacturerMetaId",tenant).append("kind",kind).append("businessId",business).append("status","QUEUED").append("payload",payload).append("dedupKey",dedup).append("createdAt",now).append("updatedAt",now);
        try{mongo.insert(doc,COLLECTION);return view(doc);}catch(DuplicateKeyException e){return view(mongo.findOne(Query.query(Criteria.where("dedupKey").is(dedup)),Document.class,COLLECTION));}
    }
    public GraphicsWorkflow findByDedupKey(String dedup) {
        Document doc = mongo.findOne(Query.query(Criteria.where("dedupKey").is(dedup)), Document.class, COLLECTION);
        return doc == null ? null : view(doc);
    }
    public GraphicsWorkflow claim(String owner){
        Date now=new Date();
        Query query=Query.query(Criteria.where("status").is("QUEUED")).with(Sort.by("createdAt"));
        Document doc=mongo.findAndModify(query,new Update().set("status","RUNNING").set("owner",owner).set("updatedAt",now).set("leaseUntil",new Date(now.getTime()+120000)),FindAndModifyOptions.options().returnNew(true),Document.class,COLLECTION);
        return doc==null?null:view(doc);
    }
    public void finish(String id,String owner,String result,String error){
        Query query=Query.query(Criteria.where("_id").is(id).and("status").is("RUNNING").and("owner").is(owner));
        Date now=new Date();
        mongo.updateFirst(query,new Update().set("status",error==null?"SUCCEEDED":"FAILED").set("result",result).set("error",error).set("updatedAt",now).set("expireAt",new Date(now.getTime()+Duration.ofDays(7).toMillis())).unset("leaseUntil"),COLLECTION);
    }
    public void heartbeat(String owner,Collection<String> ids){if(!ids.isEmpty())mongo.updateMulti(Query.query(Criteria.where("_id").in(ids).and("owner").is(owner).and("status").is("RUNNING")),new Update().set("leaseUntil",new Date(System.currentTimeMillis()+120000)),COLLECTION);}
    public void failAbandoned(){
        Date now=new Date();
        // Do not replay a possibly committed production write automatically after a process crash.
        mongo.updateMulti(Query.query(Criteria.where("status").is("RUNNING").and("leaseUntil").lt(now)),new Update().set("status","RECOVERY_REQUIRED").set("error","执行进程失联；请核对业务结果后重试，避免重复生产片或制版提交").set("updatedAt",now).set("expireAt",new Date(now.getTime()+Duration.ofDays(7).toMillis())),COLLECTION);
    }
    public GraphicsWorkflow find(String id,String tenant){Document doc=mongo.findOne(Query.query(Criteria.where("_id").is(id).and("manufacturerMetaId").is(tenant)),Document.class,COLLECTION);return doc==null?null:view(doc);}
    public List<GraphicsWorkflow> list(String business,String tenant){return mongo.find(Query.query(Criteria.where("manufacturerMetaId").is(tenant).and("businessId").is(business)).with(Sort.by(Sort.Direction.DESC,"createdAt")).limit(100),Document.class,COLLECTION).stream().map(this::view).toList();}
    public long waiting(){return mongo.count(Query.query(Criteria.where("status").is("QUEUED")),COLLECTION);}
    private GraphicsWorkflow view(Document d){return new GraphicsWorkflow(d.getString("_id"),d.getString("manufacturerMetaId"),d.getString("kind"),d.getString("businessId"),d.getString("status"),d.getString("payload"),d.getString("result"),d.getString("error"),d.getDate("createdAt"),d.getDate("updatedAt"),d.getString("owner"));}
}

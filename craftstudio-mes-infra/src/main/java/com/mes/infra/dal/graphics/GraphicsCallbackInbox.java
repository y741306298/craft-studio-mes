package com.mes.infra.dal.graphics;

import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;
import java.time.Duration;
import java.util.Date;

/** Register before POST so an immediate callback cannot arrive before its correlation exists. */
@Repository
public class GraphicsCallbackInbox {
    private static final String COLLECTION="graphics_callback_inbox";
    private final MongoTemplate mongo;
    public GraphicsCallbackInbox(MongoTemplate mongo){this.mongo=mongo;}
    @PostConstruct public void indexes(){mongo.indexOps(COLLECTION).ensureIndex(new Index().on("expireAt",Sort.Direction.ASC).expire(Duration.ZERO));}
    public void expect(String taskId,String workflowId){
        mongo.upsert(Query.query(Criteria.where("_id").is(taskId)),new Update().setOnInsert("workflowId",workflowId)
                .setOnInsert("createdAt",new Date()).setOnInsert("expireAt",new Date(System.currentTimeMillis()+Duration.ofDays(7).toMillis())),COLLECTION);
        Document doc=mongo.findById(taskId,Document.class,COLLECTION);
        if(doc==null || !workflowId.equals(doc.getString("workflowId")))throw new IllegalArgumentException("图形回调关联冲突");
    }
    public String completed(String taskId){Document doc=mongo.findById(taskId,Document.class,COLLECTION);return doc==null?null:doc.getString("response");}
    public void accept(String taskId,String workflowId,String response){
        var query=Query.query(Criteria.where("_id").is(taskId).and("workflowId").is(workflowId).and("response").exists(false));
        var updated=mongo.updateFirst(query,new Update().set("response",response).set("completedAt",new Date()),COLLECTION);
        if(updated.getModifiedCount()==1)return;
        Document existing=mongo.findById(taskId,Document.class,COLLECTION);
        if(existing==null || !workflowId.equals(existing.getString("workflowId")))throw new IllegalArgumentException("未知或过期的图形回调");
        if(!response.equals(existing.getString("response")))throw new IllegalArgumentException("图形回调结果与已接收结果冲突");
    }
}

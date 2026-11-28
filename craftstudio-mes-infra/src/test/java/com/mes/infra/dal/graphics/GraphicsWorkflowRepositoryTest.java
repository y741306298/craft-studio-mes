package com.mes.infra.dal.graphics;

import com.mongodb.client.*;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class GraphicsWorkflowRepositoryTest {
    private MongoServer server;
    private MongoClient client;
    private MongoTemplate mongo;
    private GraphicsWorkflowRepository repository;
    @BeforeEach void setup(){server=new MongoServer(new MemoryBackend());var address=server.bind();client=MongoClients.create("mongodb://127.0.0.1:"+address.getPort()+"/?retryWrites=false");mongo=new MongoTemplate(client,"graphics_test");repository=new GraphicsWorkflowRepository(mongo);repository.indexes();}
    @AfterEach void close(){client.close();server.shutdownNow();}
    @Test void concurrentDuplicateSubmissionsCreateOnlyOneDurableWorkflow()throws Exception{
        try(var pool=Executors.newFixedThreadPool(2)){
            var start=new CountDownLatch(1);
            Callable<String> submit=()->{start.await();return repository.insert(UUID.randomUUID().toString(),"factory","CONFIRM_LAYOUT","layout","{}","same-key").id();};
            var first=pool.submit(submit);var second=pool.submit(submit);start.countDown();
            assertEquals(first.get(5,TimeUnit.SECONDS),second.get(5,TimeUnit.SECONDS));
            var job=repository.claim("worker-one");assertNotNull(job);assertNull(repository.claim("worker-two"));
        }
    }
    @Test void abandonedWorkIsNotBlindlyReplayedAndResultsSurviveRepositoryRestart(){
        var inserted=repository.insert("job","factory","CONFIRM_LAYOUT","layout","{}","key");
        assertNull(repository.find(inserted.id(),"other-factory"));assertNotNull(repository.claim("old-instance"));
        mongo.updateFirst(Query.query(Criteria.where("_id").is("job")),new Update().set("leaseUntil",new Date(0)),"graphics_workflow");
        repository.failAbandoned();assertEquals("RECOVERY_REQUIRED",repository.find("job","factory").status());assertNull(repository.claim("new-instance"));
        repository.insert("success","factory","CONFIRM_LAYOUT","layout","{}","new-key");repository.claim("new-instance");
        repository.finish("success","wrong-owner","{}",null);assertEquals("RUNNING",repository.find("success","factory").status());
        repository.finish("success","new-instance","{\"success\":true}",null);
        assertEquals("SUCCEEDED",new GraphicsWorkflowRepository(mongo).find("success","factory").status());
        assertEquals("success",new GraphicsWorkflowRepository(mongo).findByDedupKey("new-key").id());
        assertNull(repository.findByDedupKey("unknown"));
    }
    @Test void callbackInboxIsDurableIdempotentAndRejectsForeignOrChangedResults(){
        var inbox=new GraphicsCallbackInbox(mongo);inbox.indexes();inbox.expect("task","workflow");
        assertNull(inbox.completed("task"));
        inbox.accept("task","workflow","{\"status\":\"success\"}");
        inbox.accept("task","workflow","{\"status\":\"success\"}");
        assertEquals("{\"status\":\"success\"}",new GraphicsCallbackInbox(mongo).completed("task"));
        assertThrows(IllegalArgumentException.class,()->inbox.accept("task","foreign","{}"));
        assertThrows(IllegalArgumentException.class,()->inbox.accept("task","workflow","{\"status\":\"error\"}"));
        assertThrows(IllegalArgumentException.class,()->inbox.accept("unknown","workflow","{}"));
        assertThrows(IllegalArgumentException.class,()->inbox.expect("task","foreign"));
        assertEquals("{\"status\":\"success\"}",inbox.completed("task"));
    }
}

package com.mes.application.command.delivery.packaging;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.mongodb.client.result.UpdateResult;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PackagingTaskStoreTest {
    @Test void ambiguousPrintFailureRetainsReservationForReview() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        PackagingTaskStore store = new PackagingTaskStore(mongo);
        when(mongo.findOne(any(Query.class), eq(Document.class), eq(PackagingTaskStore.TASKS)))
                .thenReturn(new Document("_id", "job").append("state", "PREPARING").append("orderKey", "order").append("sideEffectPossible", true));
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(PackagingTaskStore.TASKS)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        store.failPreparation("job");
        verify(mongo).updateFirst(any(Query.class), argThat((Update u) -> "REVIEW".equals(u.getUpdateObject().get("$set", Document.class).get("state"))), eq(PackagingTaskStore.TASKS));
        verify(mongo, never()).remove(any(Query.class), eq(PackagingTaskStore.LOCKS));
    }
    @Test void workerCompletionMustOwnLeaseBeforeReleasingReservation() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        PackagingTaskStore store = new PackagingTaskStore(mongo);
        Document job = new Document("_id", "job").append("owner", "lease-owner").append("orderKey", "order");
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(PackagingTaskStore.TASKS)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, null));
        assertThrows(IllegalStateException.class, () -> store.done(job));
        verify(mongo, never()).remove(any(Query.class), eq(PackagingTaskStore.LOCKS));
        verify(mongo).updateFirst(argThat((Query q) -> "lease-owner".equals(q.getQueryObject().get("owner"))
                && "PROCESSING".equals(q.getQueryObject().get("state"))), any(Update.class), eq(PackagingTaskStore.TASKS));
    }
}

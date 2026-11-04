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
    @Test void requestSuccessTransitionAtomicallyMarksQuantitiesAppliedAndQueuesOnlyAggregation() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        PackagingTaskStore store = new PackagingTaskStore(mongo);
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(PackagingTaskStore.TASKS)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        store.ready(new Document("_id", "job").append("owner", "sync-owner"));
        verify(mongo).updateFirst(argThat((Query q) -> "PROCESSING".equals(q.getQueryObject().get("state"))
                        && "sync-owner".equals(q.getQueryObject().get("owner"))),
                argThat((Update u) -> {
                    Document set = u.getUpdateObject().get("$set", Document.class);
                    return Boolean.TRUE.equals(set.getBoolean("quantitiesApplied")) && "READY".equals(set.getString("state"));
                }), eq(PackagingTaskStore.TASKS));
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
    @Test void terminalReservationIsReleasedImmediatelyForNextBatch() {
        for (String state : java.util.List.of("DONE", "FAILED", "CLOSED")) {
            MongoTemplate mongo = mock(MongoTemplate.class);
            when(mongo.insert(any(Document.class), eq(PackagingTaskStore.LOCKS)))
                    .thenThrow(new org.springframework.dao.DuplicateKeyException("old lock")).thenAnswer(i -> i.getArgument(0));
            when(mongo.findOne(any(Query.class), eq(Document.class), eq(PackagingTaskStore.LOCKS)))
                    .thenReturn(new Document("taskId", "old-task"));
            when(mongo.findOne(any(Query.class), eq(Document.class), eq(PackagingTaskStore.TASKS)))
                    .thenReturn(new Document("state", state));
            new PackagingTaskStore(mongo).lock("order", "new-task");
            verify(mongo).remove(argThat((Query q) -> "old-task".equals(q.getQueryObject().get("taskId"))
                    && "order".equals(q.getQueryObject().get("_id"))), eq(PackagingTaskStore.LOCKS));
            verify(mongo, times(2)).insert(any(Document.class), eq(PackagingTaskStore.LOCKS));
        }
    }

    @Test void activeReservationCannotBeReleasedByAnotherBatch() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.insert(any(Document.class), eq(PackagingTaskStore.LOCKS)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("busy"));
        when(mongo.findOne(any(Query.class), eq(Document.class), eq(PackagingTaskStore.LOCKS)))
                .thenReturn(new Document("taskId", "active-task"));
        when(mongo.findOne(any(Query.class), eq(Document.class), eq(PackagingTaskStore.TASKS)))
                .thenReturn(new Document("state", "REVIEW"));
        assertThrows(IllegalStateException.class, () -> new PackagingTaskStore(mongo).lock("order", "new-task"));
        verify(mongo, never()).remove(any(Query.class), eq(PackagingTaskStore.LOCKS));
    }

    @Test void concurrentNewBatchWinningInsertIsNotUnlocked() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.insert(any(Document.class), eq(PackagingTaskStore.LOCKS)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("busy"));
        when(mongo.findOne(any(Query.class), eq(Document.class), eq(PackagingTaskStore.LOCKS)))
                .thenReturn(new Document("taskId", "old-task"), new Document("taskId", "winning-task"));
        when(mongo.findOne(any(Query.class), eq(Document.class), eq(PackagingTaskStore.TASKS)))
                .thenReturn(new Document("state", "DONE"));
        var failure = assertThrows(IllegalStateException.class,
                () -> new PackagingTaskStore(mongo).lock("order", "losing-task"));
        assertTrue(failure.getMessage().contains("winning-task"));
        verify(mongo, times(1)).remove(argThat((Query q) -> "old-task".equals(q.getQueryObject().get("taskId"))),
                eq(PackagingTaskStore.LOCKS));
    }

}

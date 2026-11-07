package com.mes.application.command.delivery.packaging;

import com.mongodb.client.result.UpdateResult;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PackagingReviewClearTest {
    @Test void scopesBothTransitionsAndPreservesPackagesAndReceipts() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.updateMulti(any(Query.class), any(Update.class), eq(PackagingTaskStore.TASKS)))
                .thenReturn(UpdateResult.acknowledged(2, 2L, null), UpdateResult.acknowledged(3, 3L, null));

        Document result = new PackagingTaskStore(mongo).clearReviewLocks("factory", "order", "task", "checked");

        assertEquals(5L, result.getLong("clearedCount"));
        assertEquals(2L, result.getLong("closedCount"));
        assertEquals(3L, result.getLong("retryCount"));
        var queries = ArgumentCaptor.forClass(Query.class);
        var updates = ArgumentCaptor.forClass(Update.class);
        verify(mongo, times(2)).updateMulti(queries.capture(), updates.capture(), eq(PackagingTaskStore.TASKS));
        for (Query query : queries.getAllValues()) {
            Document scope = query.getQueryObject();
            assertEquals("factory", scope.getString("manufacturerId"));
            assertEquals("REVIEW", scope.getString("state"));
            assertEquals("order", scope.getString("orderId"));
            assertEquals("task", scope.getString("_id"));
        }
        assertNull(queries.getAllValues().get(0).getQueryObject().get("result"));
        assertEquals(new Document("$ne", null), queries.getAllValues().get(1).getQueryObject().get("result"));
        Document closed = updates.getAllValues().get(0).getUpdateObject().get("$set", Document.class);
        Document retry = updates.getAllValues().get(1).getUpdateObject().get("$set", Document.class);
        assertEquals("CLOSED", closed.getString("state"));
        assertEquals("RETRY", retry.getString("state"));
        assertEquals(0, retry.getInteger("attempts"));
        assertNotNull(retry.getDate("nextAttemptAt"));
        for (Update update : updates.getAllValues()) {
            assertEquals(java.util.Set.of("$set"), update.getUpdateObject().keySet());
            Document fields = update.getUpdateObject().get("$set", Document.class);
            assertEquals("checked", fields.getString("reviewNote"));
            assertFalse(fields.containsKey("result"));
            assertFalse(fields.containsKey("quantitiesApplied"));
        }
        verifyNoMoreInteractions(mongo);
    }

    @Test void factoryWideClearStillOnlyTargetsReviewAndReturnsZeroWhenNothingMatches() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.updateMulti(any(Query.class), any(Update.class), eq(PackagingTaskStore.TASKS)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, null));
        Document result = new PackagingTaskStore(mongo).clearReviewLocks("factory", null, null, "checked");
        assertEquals(0L, result.getLong("clearedCount"));
        var queries = ArgumentCaptor.forClass(Query.class);
        verify(mongo, times(2)).updateMulti(queries.capture(), any(Update.class), eq(PackagingTaskStore.TASKS));
        for (Query query : queries.getAllValues()) {
            assertEquals("factory", query.getQueryObject().getString("manufacturerId"));
            assertEquals("REVIEW", query.getQueryObject().getString("state"));
            assertFalse(query.getQueryObject().containsKey("orderId"));
            assertFalse(query.getQueryObject().containsKey("_id"));
        }
    }

    @Test void missingFactoryOrReviewNoteCannotMutateAnyTasks() {
        PackagingTaskStore store = mock(PackagingTaskStore.class);
        PackagingOperation operation = new PackagingOperation(store, mock(PackagingTaskWorker.class));
        assertThrows(BusinessNotAllowException.class, () -> operation.clearReviewLocks(null, null, null, "checked"));
        assertThrows(BusinessNotAllowException.class, () -> operation.clearReviewLocks(" ", null, null, "checked"));
        assertThrows(BusinessNotAllowException.class, () -> operation.clearReviewLocks("factory", null, null, null));
        assertThrows(BusinessNotAllowException.class, () -> operation.clearReviewLocks("factory", null, null, " "));
        verifyNoInteractions(store);
    }
}

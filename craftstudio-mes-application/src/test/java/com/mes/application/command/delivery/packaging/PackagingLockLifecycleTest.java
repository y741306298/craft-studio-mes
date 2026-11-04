package com.mes.application.command.delivery.packaging;

import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import com.mes.application.dto.req.delivery.DeliveryPkgAddRequest;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PackagingLockLifecycleTest {
    private DeliveryPkgAddRequest request() {
        var request = new DeliveryPkgAddRequest();
        request.setManufacturerMetaId("factory"); request.setRequestId("batch");
        var piece = new DeliveryPkgPieceVO(); piece.setProductionPieceId("piece"); piece.setOrderId("order");
        var item = new DeliveryPkgAddRequest.DeliveryPkgPieceItem(); item.setPiece(piece); item.setQuantity(1);
        request.setPieces(List.of(item)); return request;
    }
    private PackagingTaskStore requestStore() {
        PackagingTaskStore store = mock(PackagingTaskStore.class);
        when(store.create(any())).thenAnswer(i -> i.getArgument(0));
        when(store.lock(anyString(), anyString())).thenReturn("execution-owner");
        return store;
    }
    @Test void successfulRequestReleasesBeforeBackgroundCompletion() {
        PackagingTaskStore store = requestStore();
        when(store.publish(anyString(), any())).thenReturn(new Document());
        var result = new PackagingOperation(store, mock(PackagingTaskWorker.class)).execute(request(), DeliveryPkg::new);
        assertEquals("READY", result.getPackagingState());
        verify(store).release(anyString(), eq("execution-owner"));
        assertFalse(PackagingOperation.active());
    }
    @Test void failedPrintAndErrorBothReleaseExecutionLock() {
        for (boolean error : List.of(false, true)) {
            PackagingTaskStore store = requestStore();
            var operation = new PackagingOperation(store, mock(PackagingTaskWorker.class));
            assertThrows(Throwable.class, () -> operation.execute(request(), () -> {
                if (error) throw new AssertionError("failed");
                throw new IllegalStateException("failed");
            }));
            verify(store).failPreparation(anyString());
            verify(store).release(anyString(), eq("execution-owner"));
        }
    }
    @Test void manualSuccessAndFailureBothReleaseLock() {
        for (boolean fail : List.of(false, true)) {
            PackagingTaskStore store = requestStore();
            var operation = new PackagingOperation(store, mock(PackagingTaskWorker.class));
            if (fail) {
                assertThrows(IllegalArgumentException.class, () -> operation.exclusive("factory", "order", () -> {
                    throw new IllegalArgumentException("validation failed");
                }));
                verify(store).reviewManual(anyString());
            } else {
                assertEquals("ok", operation.exclusive("factory", "order", () -> "ok"));
                verify(store).finishManual(anyString(), anyString());
            }
            verify(store).release(anyString(), eq("execution-owner"));
        }
    }
    @Test void failureRecordingManualReviewStillReleasesLock() {
        PackagingTaskStore store = requestStore();
        doThrow(new IllegalStateException("database unavailable")).when(store).reviewManual(anyString());
        assertThrows(IllegalStateException.class, () -> new PackagingOperation(store, mock(PackagingTaskWorker.class))
                .exclusive("factory", "order", () -> { throw new IllegalArgumentException("failed"); }));
        verify(store).release(anyString(), eq("execution-owner"));
    }
    @Test void releaseOnlyDeletesTheExecutionOwnerAndShutdownCleansLocalOwners() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        var store = new PackagingTaskStore(mongo);
        String oldOwner = store.lock("order", "same-task");
        String newOwner = store.lock("order", "same-task");
        assertNotEquals(oldOwner, newOwner);
        clearInvocations(mongo);
        store.release("order", oldOwner);
        store.releaseProcessLocks();
        verify(mongo).remove(argThat((Query q) -> oldOwner.equals(q.getQueryObject().get("owner"))), eq(PackagingTaskStore.LOCKS));
        verify(mongo).remove(argThat((Query q) -> newOwner.equals(q.getQueryObject().get("owner"))), eq(PackagingTaskStore.LOCKS));
    }
    @Test void clearAllOnlyDeletesLockCollectionAndReturnsDeletedCount() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.remove(any(Query.class), eq(PackagingTaskStore.LOCKS))).thenReturn(DeleteResult.acknowledged(7));
        assertEquals(7, new PackagingTaskStore(mongo).clearAllLocks());
        verify(mongo).remove(argThat((Query q) -> q.getQueryObject().isEmpty()), eq(PackagingTaskStore.LOCKS));
        verifyNoMoreInteractions(mongo);
    }
    @Test void workerFailureReleasesLockAndBusyOrderDoesNotConsumeRetryBudget() {
        for (boolean busy : List.of(false, true)) {
            PackagingTaskStore store = mock(PackagingTaskStore.class);
            Document job = new Document("_id", "task").append("orderKey", "order");
            when(store.claim()).thenReturn(job, (Document) null);
            PackagingTaskWorker worker = spy(new PackagingTaskWorker(store));
            if (busy) when(store.lock("order", "task")).thenThrow(new IllegalStateException("busy"));
            else {
                when(store.lock("order", "task")).thenReturn("worker-owner");
                doThrow(new IllegalStateException("failed")).when(worker).apply(job);
            }
            worker.tick();
            if (busy) {
                verify(store).deferForBusyOrder(job);
                verify(store, never()).retry(any(), any());
                verify(worker, never()).apply(any());
            } else {
                verify(store).retry(eq(job), any());
                verify(store).release("order", "worker-owner");
            }
        }
    }
    @Test void renewalRequiresUnexpiredOwnerAndInsertionHasBoundedLease() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.updateFirst(any(Query.class), any(org.springframework.data.mongodb.core.query.Update.class), eq(PackagingTaskStore.LOCKS)))
                .thenReturn(com.mongodb.client.result.UpdateResult.acknowledged(1, 1L, null));
        var store = new PackagingTaskStore(mongo);
        String owner = store.lock("order", "task");
        verify(mongo).insert(argThat((Document d) -> owner.equals(d.getString("owner"))
                && d.getDate("leaseUntil").getTime() - d.getDate("createdAt").getTime() == 120_000), eq(PackagingTaskStore.LOCKS));
        store.renewExecutionLocks();
        verify(mongo).updateFirst(argThat((Query q) -> owner.equals(q.getQueryObject().get("owner"))
                        && q.getQueryObject().get("leaseUntil", Document.class).containsKey("$gt")),
                any(org.springframework.data.mongodb.core.query.Update.class), eq(PackagingTaskStore.LOCKS));
    }
    @Test void oldExecutionCannotBorrowNewExecutionsLockForBackgroundWrites() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        var store = new PackagingTaskStore(mongo);
        String oldOwner = store.lock("order", "task");
        store.lock("order", "task");
        clearInvocations(mongo);
        assertThrows(IllegalStateException.class, () -> store.heartbeat(new Document("orderKey", "order")
                .append("executionLockOwner", oldOwner)));
        verifyNoInteractions(mongo);
    }

}

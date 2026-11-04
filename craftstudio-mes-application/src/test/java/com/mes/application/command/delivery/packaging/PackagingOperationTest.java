package com.mes.application.command.delivery.packaging;

import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import com.mes.application.dto.req.delivery.DeliveryPkgAddRequest;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PackagingOperationTest {
    private DeliveryPkgAddRequest request() {
        var request = new DeliveryPkgAddRequest();
        request.setManufacturerMetaId("manufacturer"); request.setRequestId("action-1");
        var piece = new DeliveryPkgPieceVO(); piece.setProductionPieceId("piece-1"); piece.setOrderId("order-1");
        var item = new DeliveryPkgAddRequest.DeliveryPkgPieceItem(); item.setPiece(piece); item.setQuantity(1);
        request.setPieces(List.of(item)); return request;
    }
    @Test void persistsTaskBeforeExternalWorkAndPublishesBeforeReturning() {
        PackagingTaskStore store = mock(PackagingTaskStore.class);
        when(store.create(any())).thenAnswer(i -> i.getArgument(0));
        PackagingTaskWorker worker = mock(PackagingTaskWorker.class);
        Document claimed = new Document("_id", "job").append("owner", "sync-owner").append("state", "PROCESSING");
        when(store.publish(anyString(), any())).thenReturn(claimed);
        PackagingOperation operation = new PackagingOperation(store, worker);
        DeliveryPkg pkg = new DeliveryPkg(); pkg.setDeliveryPkgId("pkg");
        DeliveryPkg result = operation.execute(request(), () -> {
            assertTrue(PackagingOperation.active());
            operation.risk();
            verify(store).create(any()); verify(store).lock(anyString(), anyString());
            verify(store, never()).publish(anyString(), any());
            return pkg;
        });
        assertEquals("READY", result.getPackagingState());
        assertNotNull(result.getPackagingTaskId());
        var sequence = inOrder(store, worker);
        sequence.verify(store).publish(result.getPackagingTaskId(), pkg);
        sequence.verify(worker).applyQuantities(claimed);
        sequence.verify(store).ready(claimed);
        verify(store, never()).unlock(anyString(), anyString()); // Reservation remains held until worker completion.
        assertFalse(PackagingOperation.active());
    }
    @Test void duplicateSubmissionReturnsPersistedResultWithoutReprinting() {
        PackagingTaskStore store = mock(PackagingTaskStore.class);
        var request = request();
        Document saved = new Document("_id", "job").append("pkgId", "existing-pkg")
                .append("fingerprint", PackagingOperation.fingerprint(request)).append("state", "DONE").append("result", new Document());
        when(store.create(any())).thenReturn(saved);
        DeliveryPkg pkg = new DeliveryPkg(); when(store.result(saved)).thenReturn(pkg);
        var result = new PackagingOperation(store, mock(PackagingTaskWorker.class)).execute(request, () -> { fail("must not print twice"); return null; });
        assertSame(pkg, result); assertEquals("DONE", result.getPackagingState());
        verify(store, never()).lock(anyString(), anyString());
    }
    @Test void synchronousWriteFailureNeverReturnsSuccessAndLeavesDurableRecoveryTask() {
        PackagingTaskStore store = mock(PackagingTaskStore.class);
        PackagingTaskWorker worker = mock(PackagingTaskWorker.class);
        when(store.create(any())).thenAnswer(i -> i.getArgument(0));
        Document claimed = new Document("_id", "job").append("owner", "sync-owner").append("state", "PROCESSING");
        when(store.publish(anyString(), any())).thenReturn(claimed);
        RuntimeException conflict = new IllegalStateException("one piece changed concurrently");
        doThrow(conflict).when(worker).applyQuantities(claimed);
        PackagingOperation operation = new PackagingOperation(store, worker);
        assertSame(conflict, assertThrows(RuntimeException.class, () -> operation.execute(request(), DeliveryPkg::new)));
        verify(store).retry(claimed, conflict);
        verify(store, never()).ready(any());
        verify(store, never()).unlock(anyString(), anyString());
        assertFalse(PackagingOperation.active());
    }
    @Test void duplicateCannotReturnPrintingSuccessBeforeSelectedPiecesAreVisibleAsPacked() {
        PackagingTaskStore store = mock(PackagingTaskStore.class);
        var request = request();
        Document saved = new Document("_id", "job").append("pkgId", "existing-pkg")
                .append("fingerprint", PackagingOperation.fingerprint(request)).append("state", "PROCESSING")
                .append("result", new Document()).append("quantitiesApplied", false);
        when(store.create(any())).thenReturn(saved);
        PackagingOperation operation = new PackagingOperation(store, mock(PackagingTaskWorker.class));
        assertThrows(RuntimeException.class, () -> operation.execute(request, () -> { fail("must not reprint"); return null; }));
        verify(store, never()).result(any());
        verify(store, never()).lock(anyString(), anyString());
        // Once all selected pieces are durable, the same request can replay without waiting for aggregate completion.
        saved.put("quantitiesApplied", true);
        saved.put("state", "READY");
        DeliveryPkg pkg = new DeliveryPkg(); when(store.result(saved)).thenReturn(pkg);
        assertSame(pkg, operation.execute(request, () -> { fail("must not reprint"); return null; }));
    }

    @Test void rejectsReusedKeyWithDifferentPayloadAndClearsContextOnFailure() {
        PackagingTaskStore store = mock(PackagingTaskStore.class);
        when(store.create(any())).thenReturn(new Document("fingerprint", "different"));
        assertThrows(RuntimeException.class, () -> new PackagingOperation(store, mock(PackagingTaskWorker.class)).execute(request(), DeliveryPkg::new));
        verify(store, never()).lock(anyString(), anyString());
        reset(store); when(store.create(any())).thenAnswer(i -> i.getArgument(0));
        RuntimeException failure = new IllegalStateException("ambiguous print response");
        assertSame(failure, assertThrows(RuntimeException.class, () -> new PackagingOperation(store, mock(PackagingTaskWorker.class)).execute(request(), () -> { throw failure; })));
        verify(store).failPreparation(anyString());
        assertFalse(PackagingOperation.active());
    }
    @Test void failedRequestsCanBeResubmittedWithOrWithoutRequestIdAndReplayTheirSuccessor() {
        for (String requestId : new String[]{"action-1", null}) {
            PackagingTaskStore store = mock(PackagingTaskStore.class);
            var request = request(); request.setRequestId(requestId);
            var tasks = new java.util.HashMap<String, Document>();
            when(store.create(any())).thenAnswer(invocation -> {
                Document fresh = invocation.getArgument(0);
                if (tasks.isEmpty()) {
                    tasks.put(fresh.getString("_id"), new Document(fresh).append("state", "FAILED").append("pkgId", "failed-pkg"));
                }
                return tasks.computeIfAbsent(fresh.getString("_id"), key -> fresh);
            });
            when(store.publish(anyString(), any())).thenAnswer(invocation -> {
                Document job = tasks.get(invocation.getArgument(0));
                job.put("result", new Document());
                return job;
            });
            DeliveryPkg pkg = new DeliveryPkg(); pkg.setDeliveryPkgId("new-pkg");
            when(store.result(any())).thenReturn(pkg);
            var operation = new PackagingOperation(store, mock(PackagingTaskWorker.class));
            var result = operation.execute(request, () -> pkg);
            assertEquals(2, tasks.size());
            Document failed = tasks.values().stream().filter(t -> "FAILED".equals(t.getString("state"))).findFirst().orElseThrow();
            assertNotEquals(failed.getString("_id"), result.getPackagingTaskId());
            assertEquals("failed-pkg", failed.getString("pkgId"));
            assertSame(pkg, operation.execute(request, () -> { fail("must replay successful retry"); return null; }));
            verify(store, times(1)).publish(anyString(), any());
            verify(store, times(1)).lock(anyString(), eq(result.getPackagingTaskId()));
        }
    }

    @Test void concurrentRetryUsesExistingSuccessorWithoutPrintingAgain() {
        PackagingTaskStore store = mock(PackagingTaskStore.class);
        var request = request();
        Document failed = new Document("pkgId", "failed-pkg").append("state", "FAILED")
                .append("fingerprint", PackagingOperation.fingerprint(request));
        Document pending = new Document("pkgId", "pending-pkg").append("state", "PREPARING")
                .append("fingerprint", PackagingOperation.fingerprint(request));
        when(store.create(any())).thenReturn(failed, pending);
        assertThrows(RuntimeException.class, () -> new PackagingOperation(store, mock(PackagingTaskWorker.class))
                .execute(request, () -> { fail("another submission owns retry"); return null; }));
        verify(store, never()).lock(anyString(), anyString());
    }

    @Test void ambiguousOrPartiallyAppliedTasksCannotStartAnotherPrint() {
        for (String state : List.of("REVIEW", "RETRY", "PROCESSING", "FAILED")) {
            PackagingTaskStore store = mock(PackagingTaskStore.class);
            var request = request();
            Document existing = new Document("pkgId", "existing").append("state", state)
                    .append("fingerprint", PackagingOperation.fingerprint(request));
            if (!"REVIEW".equals(state)) existing.put("result", new Document());
            when(store.create(any())).thenReturn(existing);
            assertThrows(RuntimeException.class, () -> new PackagingOperation(store, mock(PackagingTaskWorker.class))
                    .execute(request, () -> { fail("must recover original task"); return null; }));
            verify(store, never()).unlock(anyString(), anyString());
            verify(store, times(1)).create(any());
        }
    }

    @Test void newBatchAndRedoPieceHaveIndependentRequestIdentitiesWithinSameOrder() {
        PackagingTaskStore store = mock(PackagingTaskStore.class);
        when(store.create(any())).thenAnswer(i -> i.getArgument(0));
        when(store.publish(anyString(), any())).thenReturn(new Document());
        var operation = new PackagingOperation(store, mock(PackagingTaskWorker.class));
        var first = request();
        String firstId = operation.execute(first, DeliveryPkg::new).getPackagingTaskId();
        var second = request(); second.setRequestId("action-2");
        String secondId = operation.execute(second, DeliveryPkg::new).getPackagingTaskId();
        var redo = request(); redo.setRequestId(null);
        redo.getPieces().getFirst().getPiece().setProductionPieceId("redo-piece");
        String redoId = operation.execute(redo, DeliveryPkg::new).getPackagingTaskId();
        assertNotEquals(firstId, secondId);
        assertNotEquals(firstId, redoId);
        assertNotEquals(secondId, redoId);
        verify(store, times(3)).lock(anyString(), anyString());
    }

    @Test void closedReviewedAttemptCanStartNewPreparation() {
        PackagingTaskStore store = mock(PackagingTaskStore.class);
        var request = request();
        Document closed = new Document("pkgId", "closed-pkg").append("state", "CLOSED")
                .append("sideEffectPossible", true).append("fingerprint", PackagingOperation.fingerprint(request));
        when(store.create(any())).thenReturn(closed).thenAnswer(i -> i.getArgument(0));
        when(store.publish(anyString(), any())).thenReturn(new Document());
        var result = new PackagingOperation(store, mock(PackagingTaskWorker.class)).execute(request, DeliveryPkg::new);
        assertEquals("READY", result.getPackagingState());
        verify(store, times(2)).create(any());
        verify(store).lock(anyString(), eq(result.getPackagingTaskId()));
    }

}

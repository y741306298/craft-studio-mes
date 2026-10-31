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
}

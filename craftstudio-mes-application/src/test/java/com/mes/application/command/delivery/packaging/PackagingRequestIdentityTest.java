package com.mes.application.command.delivery.packaging;

import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import com.mes.application.dto.req.delivery.DeliveryPkgAddRequest;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PackagingRequestIdentityTest {
    private final PackagingTaskStore store = mock(PackagingTaskStore.class);
    private final PackagingTaskWorker worker = mock(PackagingTaskWorker.class);
    private final PackagingOperation operation = new PackagingOperation(store, worker);
    private final Map<String, Document> tasks = new LinkedHashMap<>();
    private final Map<String, DeliveryPkg> results = new LinkedHashMap<>();

    @BeforeEach void persistRequestIdentities() {
        when(store.create(any())).thenAnswer(invocation -> {
            Document task = invocation.getArgument(0);
            return tasks.computeIfAbsent(task.getString("_id"), ignored -> task);
        });
        when(store.lock(anyString(), anyString())).thenReturn("execution-owner");
        when(store.publish(anyString(), any())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            DeliveryPkg pkg = invocation.getArgument(1);
            results.put(id, pkg);
            return tasks.get(id).append("result", new Document("deliveryPkgId", pkg.getDeliveryPkgId()))
                    .append("state", "PROCESSING").append("quantitiesApplied", false);
        });
        when(store.result(any())).thenAnswer(invocation -> results.get(((Document) invocation.getArgument(0)).getString("_id")));
        doAnswer(invocation -> {
            tasks.get(invocation.getArgument(0)).put("sideEffectPossible", true);
            return null;
        }).when(store).risk(anyString());
        doAnswer(invocation -> {
            Document task = tasks.get(invocation.getArgument(0));
            if ("PREPARING".equals(task.getString("state")))
                task.put("state", Boolean.TRUE.equals(task.getBoolean("sideEffectPossible")) ? "REVIEW" : "FAILED");
            return null;
        }).when(store).failPreparation(anyString());
    }

    private DeliveryPkgAddRequest request(String requestId, String pieceId) {
        var request = new DeliveryPkgAddRequest();
        request.setManufacturerMetaId("factory");
        request.setRequestId(requestId);
        var piece = new DeliveryPkgPieceVO();
        piece.setProductionPieceId(pieceId);
        piece.setOrderId("order");
        var item = new DeliveryPkgAddRequest.DeliveryPkgPieceItem();
        item.setPiece(piece);
        item.setQuantity(1);
        request.setPieces(List.of(item));
        return request;
    }

    private DeliveryPkg newPackage() {
        var pkg = new DeliveryPkg();
        pkg.setDeliveryPkgId(PackagingOperation.pkgId());
        return pkg;
    }

    @Test void legacyRequestsWithoutIdsStartIndependentPackages() {
        for (String requestId : Arrays.asList(null, "", " ", "\t\n")) {
            DeliveryPkg first = operation.execute(request(requestId, "piece"), this::newPackage);
            DeliveryPkg next = operation.execute(request(requestId, "piece"), this::newPackage);
            assertNotEquals(first.getPackagingTaskId(), next.getPackagingTaskId());
            assertNotEquals(first.getDeliveryPkgId(), next.getDeliveryPkgId());
            assertEquals("READY", next.getPackagingState());
        }
        assertEquals(8, tasks.size());
        verify(store, times(8)).release(anyString(), eq("execution-owner"));
        assertFalse(PackagingOperation.active());
    }

    @Test void legacyRequestCanProceedAfterEarlierRequestEnteredReview() {
        assertThrows(IllegalStateException.class, () -> operation.execute(request(null, "piece"), () -> {
            operation.risk();
            throw new IllegalStateException("upstream response lost");
        }));
        Document review = tasks.values().iterator().next();
        DeliveryPkg next = operation.execute(request(null, "piece"), this::newPackage);
        assertEquals("REVIEW", review.getString("state"));
        assertEquals("READY", next.getPackagingState());
        assertNotEquals(review.getString("_id"), next.getPackagingTaskId());
        verify(store, times(2)).release(anyString(), eq("execution-owner"));
    }

    @Test void identicalContentsWithNewRequestIdCreateAnotherPackageForSameOrder() {
        DeliveryPkg first = operation.execute(request("batch-1", "piece"), this::newPackage);
        DeliveryPkg second = operation.execute(request("batch-2", "piece"), this::newPackage);

        assertNotEquals(first.getPackagingTaskId(), second.getPackagingTaskId());
        assertNotEquals(first.getDeliveryPkgId(), second.getDeliveryPkgId());
        assertEquals(tasks.get(first.getPackagingTaskId()).get("orderKey"), tasks.get(second.getPackagingTaskId()).get("orderKey"));
        assertEquals(2, tasks.size());
        verify(store, times(2)).lock(anyString(), anyString());
        verify(store, times(2)).release(anyString(), eq("execution-owner"));
        verify(worker, times(2)).applyQuantities(any());
    }

    @Test void redoPiecesCanBePackagedAgainUnderSameOrderWithNewRequestId() {
        DeliveryPkg original = operation.execute(request("original-batch", "piece"), this::newPackage);
        DeliveryPkg redo = operation.execute(request("redo-batch", "redo-piece"), this::newPackage);

        assertNotEquals(original.getPackagingTaskId(), redo.getPackagingTaskId());
        assertNotEquals(original.getDeliveryPkgId(), redo.getDeliveryPkgId());
        assertEquals("READY", redo.getPackagingState());
        verify(store, times(2)).release(anyString(), eq("execution-owner"));
    }

    @Test void uncertainFailureReleasesLockAndReviewHistoryDoesNotBlockNewBatch() {
        assertThrows(IllegalStateException.class, () -> operation.execute(request("uncertain-batch", "piece"), () -> {
            operation.risk();
            throw new IllegalStateException("upstream response lost");
        }));
        Document review = tasks.values().iterator().next();
        assertEquals("REVIEW", review.getString("state"));
        verify(store).release(review.getString("orderKey"), "execution-owner");
        assertFalse(PackagingOperation.active());

        DeliveryPkg next = operation.execute(request("next-batch", "piece"), this::newPackage);

        assertEquals("READY", next.getPackagingState());
        assertEquals(review.get("orderKey"), tasks.get(next.getPackagingTaskId()).get("orderKey"));
        assertNotEquals(review.getString("_id"), next.getPackagingTaskId());
        assertEquals("REVIEW", review.getString("state"));
        assertNull(review.get("result"));
        verify(store, times(2)).release(anyString(), eq("execution-owner"));
    }

    @Test void networkRetryReplaysSavedResultWithoutPrintingOrAcquiringLockAgain() {
        DeliveryPkg first = operation.execute(request("batch", "piece"), this::newPackage);
        clearInvocations(store, worker);

        DeliveryPkg retry = operation.execute(request("batch", "piece"), () -> fail("Must not print a network retry"));

        assertEquals(first.getDeliveryPkgId(), retry.getDeliveryPkgId());
        assertEquals(first.getPackagingTaskId(), retry.getPackagingTaskId());
        assertEquals(1, tasks.size());
        verify(store, never()).lock(anyString(), anyString());
        verify(store, never()).publish(anyString(), any());
        verifyNoInteractions(worker);
    }

    @Test void retryOfReviewTaskKeepsOriginalIdentityAndDoesNotPrintAgain() {
        assertThrows(IllegalStateException.class, () -> operation.execute(request("batch", "piece"), () -> {
            operation.risk();
            throw new IllegalStateException("upstream response lost");
        }));
        clearInvocations(store, worker);

        var error = assertThrows(BusinessNotAllowException.class,
                () -> operation.execute(request("batch", "piece"), () -> fail("Must reconcile the original request")));

        assertTrue(error.getMessage().contains("REVIEW"));
        assertEquals(1, tasks.size());
        verify(store, never()).lock(anyString(), anyString());
        verifyNoInteractions(worker);
    }

    @Test void retryBeforeQuantitiesAreAppliedDoesNotCreateAnotherPackage() {
        var request = request("batch", "piece");
        Document existing = new Document("_id", "task").append("fingerprint", PackagingOperation.fingerprint(request))
                .append("pkgId", "existing-pkg").append("state", "PROCESSING")
                .append("result", new Document()).append("quantitiesApplied", false);
        doReturn(existing).when(store).create(any());

        var error = assertThrows(BusinessNotAllowException.class,
                () -> operation.execute(request, () -> fail("Must await the original quantity update")));

        assertTrue(error.getMessage().contains("PROCESSING"));
        verify(store, never()).lock(anyString(), anyString());
        verifyNoInteractions(worker);
    }

    @Test void reusingRequestIdWithDifferentContentsIsRejected() {
        operation.execute(request("batch", "piece"), this::newPackage);
        clearInvocations(store, worker);

        var error = assertThrows(BusinessNotAllowException.class,
                () -> operation.execute(request("batch", "redo-piece"), () -> fail("Must use a new requestId for redo pieces")));

        assertTrue(error.getMessage().contains("requestId 已用于不同的打包内容"));
        assertEquals(1, tasks.size());
        verify(store, never()).lock(anyString(), anyString());
        verifyNoInteractions(worker);
    }
}

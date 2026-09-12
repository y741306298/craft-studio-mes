package com.mes.application.command.typesetting;

import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.vo.ReleaseLayoutResult;
import com.mes.domain.manufacturer.productionPiece.entity.PieceQuantityTransfer;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppTypesettingServiceReleaseLayoutTest {

    @Test
    void failedPieceRollbackKeepsThatLayoutAndContinuesWithNextLayout() {
        TestContext context = contextWithLayouts(
                layout("layout-1", cell("piece-1", 1), cell("broken-piece", 1)),
                layout("layout-2", cell("piece-2", 1)));
        doThrow(new IllegalStateException("rollback failed"))
                .doNothing()
                .when(context.productionPieceService).transferPieceQuantitiesBetweenNodesStrict(anyList());

        ReleaseLayoutResult result = release(context.service);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getDeletedLayoutIds()).containsExactly("layout-2");
        assertThat(result.getMessage()).contains("释放排版记录失败(layout-1)");
        verify(context.typesettingService, never()).deleteTypesetting("layout-1");
        verify(context.typesettingService).deleteTypesetting("layout-2");
    }

    @Test
    void rollsBackEachLayoutsPiecesInOneBatchBeforeDeletingThatLayout() {
        TestContext context = contextWithLayouts(
                layout("layout-1", cell("piece-1", 1), cell("piece-2", 1)),
                layout("layout-2", cell("piece-3", 1)));

        ReleaseLayoutResult result = release(context.service);

        assertThat(result.isSuccess()).isTrue();
        InOrder inOrder = inOrder(context.productionPieceService, context.typesettingService);
        inOrder.verify(context.productionPieceService).transferPieceQuantitiesBetweenNodesStrict(anyList());
        inOrder.verify(context.typesettingService).deleteTypesetting("layout-1");
        inOrder.verify(context.productionPieceService).transferPieceQuantitiesBetweenNodesStrict(anyList());
        inOrder.verify(context.typesettingService).deleteTypesetting("layout-2");
    }

    @Test
    void sumsRepeatedPieceCellsWithinLayoutAndRollsBackSharedPieceForEachLayout() {
        TestContext context = contextWithLayouts(
                layout("layout-1", cell("shared-piece", 1), cell("shared-piece", 2)),
                layout("layout-2", cell("shared-piece", 4)));
        ArgumentCaptor<List<PieceQuantityTransfer>> transfersCaptor = ArgumentCaptor.forClass(List.class);

        release(context.service);

        verify(context.productionPieceService, times(2))
                .transferPieceQuantitiesBetweenNodesStrict(transfersCaptor.capture());
        assertThat(transfersCaptor.getAllValues())
                .extracting(transfers -> transfers.get(0).getQuantity())
                .containsExactly(3, 4);
    }

    private ReleaseLayoutResult release(AppTypesettingService service) {
        return ReflectionTestUtils.invokeMethod(service, "doReleaseLayout", List.of("layout-1", "layout-2"));
    }

    private TestContext contextWithLayouts(TypesettingInfo first, TypesettingInfo second) {
        AppTypesettingService service = new AppTypesettingService();
        TypesettingService typesettingService = mock(TypesettingService.class);
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        ReflectionTestUtils.setField(service, "domainTypesettingService", typesettingService);
        ReflectionTestUtils.setField(service, "productionPieceService", productionPieceService);
        ReflectionTestUtils.setField(service, "transactionTemplate", transactionTemplate);
        when(typesettingService.findById("layout-1")).thenReturn(first);
        when(typesettingService.findById("layout-2")).thenReturn(second);
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        }).when(transactionTemplate).execute(any());
        return new TestContext(service, typesettingService, productionPieceService);
    }

    private TypesettingInfo layout(String id, TypesettingSourceCell... cells) {
        TypesettingInfo info = new TypesettingInfo();
        info.setId(id);
        info.setTypesettingId(id);
        info.setTypesettingCells(List.of(cells));
        return info;
    }

    private TypesettingSourceCell cell(String pieceId, int quantity) {
        TypesettingSourceCell cell = new TypesettingSourceCell();
        cell.setSourceType(TypesettingSourceType.PART.getCode());
        cell.setSourceId(pieceId);
        cell.setQuantity(quantity);
        return cell;
    }

    private record TestContext(AppTypesettingService service,
                               TypesettingService typesettingService,
                               ProductionPieceService productionPieceService) {
    }
}

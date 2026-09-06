package com.mes.application.command.print;

import com.mes.application.command.print.vo.PrintReportResult;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.domain.manufacturer.productionPiece.entity.PieceQuantityTransfer;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AppPrintServiceTest {

    @Test
    void repeatedReportAfterCompletionDoesNotTransferAgain() {
        TypesettingService typesettingService = mock(TypesettingService.class);
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        AppPrintService service = service(typesettingService, productionPieceService);
        TypesettingInfo completed = typesetting("layout-1", 0);
        when(typesettingService.findById("layout-1")).thenReturn(completed);

        TypesettingInfo request = new TypesettingInfo();
        request.setId("layout-1");
        request.setQuantity(1);
        PrintReportResult result = service.reportPrinting(request);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getTransferRecordCount()).isZero();
        verify(productionPieceService, never()).transferPieceQuantitiesBetweenNodes(anyList());
    }

    @Test
    void reportTransfersOnlySuccessfullyClaimedRemainingQuantity() {
        TypesettingService typesettingService = mock(TypesettingService.class);
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        AppPrintService service = service(typesettingService, productionPieceService);
        TypesettingInfo info = typesetting("layout-1", 2);
        when(typesettingService.findById("layout-1")).thenReturn(info);
        when(typesettingService.compareAndSetPrintReport(
                "layout-1", 2, 0, TypesettingStatus.COMPLETED.getCode(), null)).thenReturn(true);

        TypesettingInfo request = new TypesettingInfo();
        request.setId("layout-1");
        request.setQuantity(5);
        service.reportPrinting(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PieceQuantityTransfer>> captor = ArgumentCaptor.forClass(List.class);
        verify(productionPieceService).transferPieceQuantitiesBetweenNodes(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getQuantity()).isEqualTo(2);
    }

    private AppPrintService service(TypesettingService typesettingService,
                                    ProductionPieceService productionPieceService) {
        AppPrintService service = new AppPrintService();
        ReflectionTestUtils.setField(service, "typesettingService", typesettingService);
        ReflectionTestUtils.setField(service, "productionPieceService", productionPieceService);
        return service;
    }

    private TypesettingInfo typesetting(String id, int leaveQuantity) {
        TypesettingSourceCell cell = new TypesettingSourceCell();
        cell.setSourceType(TypesettingSourceType.PART.getCode());
        cell.setSourceId("piece-1");
        cell.setQuantity(1);
        TypesettingInfo info = new TypesettingInfo();
        info.setId(id);
        info.setLeaveQuantity(leaveQuantity);
        info.setStatus(leaveQuantity == 0
                ? TypesettingStatus.COMPLETED.getCode() : TypesettingStatus.PRINTING_IN_PROGRESS.getCode());
        info.setTypesettingCells(List.of(cell));
        return info;
    }
}

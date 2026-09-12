package com.mes.application.command.print;

import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.domain.manufacturer.productionPiece.entity.PieceQuantityTransfer;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppPrintServiceTest {

    @Test
    void skipsMissingPiecesAndUpdatesTypesettingAfterExistingPieceTransfers() {
        TypesettingService typesettingService = mock(TypesettingService.class);
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        AppPrintService service = new AppPrintService();
        ReflectionTestUtils.setField(service, "typesettingService", typesettingService);
        ReflectionTestUtils.setField(service, "productionPieceService", productionPieceService);

        TypesettingInfo stored = typesetting("typesetting-record", 1,
                sourceCell("existing-piece"), sourceCell("missing-piece"));
        TypesettingInfo request = new TypesettingInfo();
        request.setId(stored.getId());
        request.setQuantity(1);
        request.setRemark("操作员备注");
        ProductionPiece existingPiece = new ProductionPiece();
        existingPiece.setId("existing-record");
        existingPiece.setProductionPieceId("existing-piece");

        when(typesettingService.findById(stored.getId())).thenReturn(stored);
        when(productionPieceService.findByProductionPieceIds(anyCollection())).thenReturn(List.of(existingPiece));
        when(typesettingService.compareAndSetPrintReport(stored.getId(), 1, 0,
                TypesettingStatus.COMPLETED.getCode(), "操作员备注；打印报备跳过不存在的零件：missing-piece"))
                .thenReturn(true);

        service.reportPrinting(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PieceQuantityTransfer>> transfers = ArgumentCaptor.forClass(List.class);
        verify(productionPieceService).transferPieceQuantitiesBetweenNodes(transfers.capture());
        assertThat(transfers.getValue()).extracting(PieceQuantityTransfer::getProductionPieceId)
                .containsExactly("existing-piece");
        InOrder updateOrder = inOrder(productionPieceService, typesettingService);
        updateOrder.verify(productionPieceService).transferPieceQuantitiesBetweenNodes(anyList());
        updateOrder.verify(typesettingService).compareAndSetPrintReport(stored.getId(), 1, 0,
                TypesettingStatus.COMPLETED.getCode(), "操作员备注；打印报备跳过不存在的零件：missing-piece");
    }

    private TypesettingInfo typesetting(String id, int leaveQuantity, TypesettingSourceCell... cells) {
        TypesettingInfo info = new TypesettingInfo();
        info.setId(id);
        info.setTypesettingId("typesetting-file");
        info.setStatus(TypesettingStatus.PRINTING_IN_PROGRESS.getCode());
        info.setLeaveQuantity(leaveQuantity);
        info.setTypesettingCells(List.of(cells));
        return info;
    }

    private TypesettingSourceCell sourceCell(String productionPieceId) {
        TypesettingSourceCell cell = new TypesettingSourceCell();
        cell.setSourceType(TypesettingSourceType.PART.getCode());
        cell.setSourceId(productionPieceId);
        cell.setQuantity(1);
        return cell;
    }
}

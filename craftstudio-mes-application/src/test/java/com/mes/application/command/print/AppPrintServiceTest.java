package com.mes.application.command.print;

import com.mes.application.command.print.vo.PendingPrintMaterialVO;
import com.mes.application.command.print.vo.PrintReportResult;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.mes.domain.manufacturer.manufacturerMeta.service.ManufacturerDeviceCfgService;
import com.mes.domain.manufacturer.productionPiece.entity.PieceQuantityTransfer;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import com.mes.domain.order.orderInfo.vo.MaterialConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AppPrintServiceTest {

    @Test
    void pendingPrintListPassesMaterialIdToBothPrintableStatusQueries() {
        TypesettingService typesettingService = mock(TypesettingService.class);
        AppPrintService service = service(typesettingService, mock(ProductionPieceService.class));

        service.findPendingPrintTypesetting("manufacturer-1", null, null, "material-1",
                null, null, null, 1, 20);

        verify(typesettingService).findTypesettingByConditions("manufacturer-1",
                TypesettingStatus.PRINTING.getCode(), null, "material-1", null,
                null, null, null, 1, Integer.MAX_VALUE);
        verify(typesettingService).findTypesettingByConditions("manufacturer-1",
                TypesettingStatus.PRINTING_IN_PROGRESS.getCode(), null, "material-1", null,
                null, null, null, 1, Integer.MAX_VALUE);
    }

    @Test
    void pendingPrintMaterialsUsesDeviceCodeAndReturnsDistinctMaterialFields() {
        TypesettingService typesettingService = mock(TypesettingService.class);
        ManufacturerDeviceCfgService deviceService = mock(ManufacturerDeviceCfgService.class);
        AppPrintService service = service(typesettingService, mock(ProductionPieceService.class));
        ReflectionTestUtils.setField(service, "manufacturerDeviceCfgService", deviceService);
        ManufacturerDeviceCfg device = new ManufacturerDeviceCfg();
        device.setDeviceCode("printer-1");
        when(deviceService.findById("device-1")).thenReturn(device);
        Date startTime = new Date(1_000);
        Date endTime = new Date(2_000);
        when(typesettingService.findPrintableMaterials("manufacturer-1", "printer-1", startTime, endTime))
                .thenReturn(List.of(material("material-1", "铜版纸"), material("material-1", "铜版纸"),
                        material("material-2", "牛皮纸")));

        List<PendingPrintMaterialVO> result = service.findPendingPrintMaterials(
                "manufacturer-1", "device-1", startTime, endTime);

        assertThat(result).extracting(PendingPrintMaterialVO::getMaterialId,
                        PendingPrintMaterialVO::getMaterialName)
                .containsExactly(tuple("material-1", "铜版纸"), tuple("material-2", "牛皮纸"));
    }

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

    private TypesettingInfo material(String materialId, String materialName) {
        MaterialConfig.MaterialSnapshot snapshot = new MaterialConfig.MaterialSnapshot();
        snapshot.setName(materialName);
        MaterialConfig materialConfig = new MaterialConfig();
        materialConfig.setMaterialId(materialId);
        materialConfig.setMaterialSnapshot(snapshot);
        TypesettingInfo info = new TypesettingInfo();
        info.setMaterialConfig(materialConfig);
        return info;
    }
}

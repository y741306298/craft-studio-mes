package com.mes.application.command.typesetting;

import com.alibaba.fastjson2.JSON;
import com.mes.application.command.api.AlgorithmCoreApiService;
import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.api.resp.FormeGenerationResponse;
import com.mes.application.command.typesetting.layout.LargeVisionLayoutBuildService;
import com.mes.application.command.typesetting.strategy.DoubleSideMountingMirrorFormeStrategy;
import com.mes.application.dto.req.typesetting.ConfirmPrintRequest;
import com.mes.application.command.typesetting.vo.ConfirmPrintResult;
import com.mes.application.command.typesetting.vo.LayoutConfirmResult;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerDeviceCfgRepository;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.util.ProcedureFlowNodeMatcher;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingPrintTask;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.service.TypesettingPrintTaskService;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import com.piliofpala.craftstudio.shared.infra.cloud.platforms.alicloud.AliCloudAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SublimationPlateWorkflowTest {
    @Test
    void commonFlowKeepsSublimationAndBackEvenWhenPrefixDiffers() {
        AppTypesettingService service = new AppTypesettingService();
        List<ProductionPiece> pieces = List.of(
                SublimationMirrorManifestTest.piece("one", "one.tif", "热升华（单面布）", "反面相同画面"),
                SublimationMirrorManifestTest.piece("two", "two.tif", "热升华（双面布）", "反面不同画面"));
        ProcedureFlow flow = ReflectionTestUtils.invokeMethod(service, "validateAndBuildCommonProcedureFlow", pieces, List.of());
        assertTrue(ProcedureFlowNodeMatcher.hasSublimationBackImageNode(flow));
    }

    @Test
    void confirmLayoutRejectsSublimationWithoutChangingStatusOrSubmittingAlgorithm() {
        AppTypesettingService service = new AppTypesettingService();
        TypesettingService domain = mock(TypesettingService.class);
        AlgorithmCoreApiService algorithm = mock(AlgorithmCoreApiService.class);
        TypesettingInfo plate = plate("反面相同画面");
        when(domain.findById("front-db")).thenReturn(plate);
        ReflectionTestUtils.setField(service, "domainTypesettingService", domain);
        ReflectionTestUtils.setField(service, "algorithmCoreApiService", algorithm);
        LayoutConfirmResult result = ReflectionTestUtils.invokeMethod(service, "doConfirmLayout", plate);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("必须直接下发"));
        assertEquals(TypesettingStatus.CONFIRMING.getCode(), plate.getStatus());
        verifyNoInteractions(algorithm);
        verify(domain, never()).updateTypesetting(any());
    }

    @Test
    void ordinaryDoubleSideStillCannotBeDispatchedDirectly() {
        TypesettingInfo plate = plate("反面相同画面");
        plate.setProcedureFlow(SublimationMirrorManifestTest.flow("双面对裱"));
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(
                new AppTypesettingService(), "validateConfirmPrintPreconditions", plate));
    }

    @Test
    void sublimationWithoutBackImageDoesNotCreateMirrorFromOtherDoubleSideNames() {
        TypesettingInfo plate = plate("双面对裱");
        assertFalse(new DoubleSideMountingMirrorFormeStrategy().supports(plate));
    }

    @Test
    void missingSublimationBackLayoutBlocksDispatch() {
        TypesettingInfo plate = plate("反面不同画面");
        plate.getElement().setNestedMirrorSvg(null);
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(
                new AppTypesettingService(), "validateConfirmPrintPreconditions", plate));
    }

    @ParameterizedTest
    @ValueSource(strings = {"反面相同画面", "反面不同画面"})
    void oneDispatchGeneratesBothSidesAndBothCallbacksBecomePendingPrint(String backNode) {
        AppTypesettingService service = new AppTypesettingService();
        TypesettingService domain = mock(TypesettingService.class);
        AlgorithmCoreApiService algorithm = mock(AlgorithmCoreApiService.class);
        ProductionPieceService pieces = mock(ProductionPieceService.class);
        TypesettingPrintTaskService tasks = mock(TypesettingPrintTaskService.class);
        TypesettingInfo front = plate(backNode);
        Map<String, TypesettingInfo> plates = new HashMap<>();
        plates.put(front.getId(), front);
        when(domain.findById(anyString())).thenAnswer(call -> plates.get(call.getArgument(0)));
        when(domain.addTypesetting(any())).thenAnswer(call -> {
            TypesettingInfo mirror = call.getArgument(0);
            mirror.setId("mirror-db");
            plates.put(mirror.getId(), mirror);
            return mirror;
        });
        ManufacturerDeviceCfgRepository devices = mock(ManufacturerDeviceCfgRepository.class);
        ManufacturerDeviceCfg device = new ManufacturerDeviceCfg();
        device.setDeviceCode("printer");
        device.setDeviceInfoId("printer-id");
        device.setDeviceName("printer-name");
        when(devices.filterList(anyInt(), anyInt(), anyMap())).thenReturn(List.of(device));
        RestTemplate rest = mock(RestTemplate.class);
        when(rest.getForObject(any(URI.class), eq(byte[].class))).thenReturn(
                "<svg xmlns='http://www.w3.org/2000/svg' width='1000' height='2000'/>".getBytes(StandardCharsets.UTF_8));
        ProductionPiece piece = SublimationMirrorManifestTest.piece("piece", "back.tif", "热升华", "排版中", "打印中");
        piece.getProcedureFlow().getNodes().get(1).setPieceQuantity(3);
        piece.setMarks(Map.of("sublimation-border", "front-border.png"));
        when(pieces.findByIds(anyCollection())).thenReturn(Map.of("piece", piece));
        when(pieces.findById("piece")).thenReturn(piece);
        when(pieces.transferTypesettingQuantitiesToPrinting(anyMap())).thenReturn(1L);
        ReflectionTestUtils.setField(service, "domainTypesettingService", domain);
        ReflectionTestUtils.setField(service, "algorithmCoreApiService", algorithm);
        ReflectionTestUtils.setField(service, "productionPieceService", pieces);
        ReflectionTestUtils.setField(service, "typesettingPrintTaskService", tasks);
        ReflectionTestUtils.setField(service, "manufacturerDeviceCfgRepository", devices);
        ReflectionTestUtils.setField(service, "restTemplate", rest);
        ReflectionTestUtils.setField(service, "aliCloudAuthService", mock(AliCloudAuthService.class));
        ReflectionTestUtils.setField(service, "generateFormeUrl", "https://example.test/callback/forme");
        ReflectionTestUtils.setField(service, "mirrorFormeStrategies", List.of(new DoubleSideMountingMirrorFormeStrategy()));
        ReflectionTestUtils.setField(service, "layoutModeBuildServices", List.of(new LargeVisionLayoutBuildService()));
        service.initLayoutModeBuilders();

        ConfirmPrintRequest request = new ConfirmPrintRequest();
        request.setId(front.getId());
        request.setDeviceCode("printer");
        ConfirmPrintResult result = ReflectionTestUtils.invokeMethod(service, "doConfirmPrint", request);
        assertTrue(result.isSuccess(), result.getMessage());
        TypesettingInfo mirror = plates.get("mirror-db");
        assertNotNull(mirror);
        assertEquals("job-Mirror", mirror.getTypesettingId());
        assertEquals(front.getLayoutMode(), mirror.getLayoutMode());
        assertTrue(mirror.getMarks().isEmpty());
        assertEquals("https://example.test/back.svg", mirror.getElement().getNestedSvg());
        assertFalse(new DoubleSideMountingMirrorFormeStrategy().supports(mirror));
        for (TypesettingInfo plate : plates.values()) {
            assertEquals("FORME_OP:PRINT:printer", plate.getRemark());
            assertEquals(TypesettingStatus.CONFIRMED.getCode(), plate.getStatus());
        }
        ArgumentCaptor<String> submitted = ArgumentCaptor.forClass(String.class);
        verify(algorithm, times(2)).generateFormeAsync(submitted.capture(), anyString());
        for (String json : submitted.getAllValues()) {
            FormeGenerationRequest forme = JSON.parseObject(json, FormeGenerationRequest.class);
            assertTrue(forme.getForme().getMarks().isEmpty());
            assertTrue(forme.getForme().getAnchorPoints().isEmpty());
            assertEquals(0, forme.getForme().getMargin().getTop());
        }

        // 模拟反面先回调；不需要再调用确认接口，两面均直接进入待打印。
        for (String id : List.of("mirror-db", "front-db")) {
            FormeGenerationResponse response = new FormeGenerationResponse();
            response.setId(id);
            response.setStatus("success");
            response.setResult(new FormeGenerationResponse.Result());
            ReflectionTestUtils.invokeMethod(service, "doHandleGenerateFormeCallback", response, id);
            assertEquals(TypesettingStatus.PRINTING.getCode(), plates.get(id).getStatus());
            // 重复回调不能重复推进数量或创建任务。
            ReflectionTestUtils.invokeMethod(service, "doHandleGenerateFormeCallback", response, id);
        }
        verify(pieces, times(1)).transferTypesettingQuantitiesToPrinting(Map.of("piece", 3));
        ArgumentCaptor<TypesettingPrintTask> printTasks = ArgumentCaptor.forClass(TypesettingPrintTask.class);
        verify(tasks, times(2)).saveOrUpdate(printTasks.capture());
        TypesettingPrintTask mirrorTask = printTasks.getAllValues().stream()
                .filter(task -> "job-Mirror".equals(task.getTypesettingInfoId())).findFirst().orElseThrow();
        assertTrue(mirrorTask.getData().getMarks().isEmpty());
    }

    private static TypesettingInfo plate(String back) {
        TypesettingInfo plate = new TypesettingInfo();
        plate.setId("front-db");
        plate.setTypesettingId("job");
        plate.setManufacturerMetaId("factory");
        plate.setProcedureFlow(SublimationMirrorManifestTest.flow("热升华", back));
        plate.setLayoutMode(TypesettingLayoutMode.LARGE_VISION_LAYOUT.getCode());
        plate.setStatus(TypesettingStatus.CONFIRMING.getCode());
        plate.setMarks(Map.of("sublimation-border", "front-border.png"));
        TypesettingElement element = new TypesettingElement();
        element.setNestedSvg("https://example.test/front.svg");
        element.setNestedMirrorSvg("https://example.test/back.svg");
        element.setWidth(BigDecimal.valueOf(1000));
        element.setHeight(BigDecimal.valueOf(2000));
        plate.setElement(element);
        TypesettingSourceCell cell = new TypesettingSourceCell();
        cell.setSourceType("PRODUCTION_PIECE");
        cell.setSourceId("piece");
        cell.setQuantity(3);
        plate.setTypesettingCells(List.of(cell));
        return plate;
    }
}

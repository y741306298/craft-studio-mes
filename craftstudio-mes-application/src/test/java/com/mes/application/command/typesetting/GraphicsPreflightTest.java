package com.mes.application.command.typesetting;

import com.alibaba.fastjson2.JSON;
import com.mes.application.command.api.AlgorithmCoreApiService;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.layout.TypesettingLayoutModeBuildService;
import com.mes.application.command.typesetting.vo.TypesettingProductionPieceVO;
import com.mes.application.dto.req.typesetting.*;
import com.mes.domain.manufacturer.procedureFlow.entity.*;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.*;
import com.mes.domain.manufacturer.typesetting.service.*;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GraphicsPreflightTest {
    private final AppTypesettingService service = new AppTypesettingService();
    private final ProductionPieceService pieces = mock(ProductionPieceService.class);
    private final TypesettingService layouts = mock(TypesettingService.class);
    private final AlgorithmCoreApiService algorithms = mock(AlgorithmCoreApiService.class);
    private final RedisTemplate<?,?> redis = mock(RedisTemplate.class);
    private ProductionPiece piece;
    private LayoutConfirmRequest request;
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service,"productionPieceService",pieces);
        ReflectionTestUtils.setField(service,"domainTypesettingService",layouts);
        ReflectionTestUtils.setField(service,"algorithmCoreApiService",algorithms);
        ReflectionTestUtils.setField(service,"redisTemplate",redis);
        piece=new ProductionPiece();piece.setId("piece");piece.setProductionPieceId("PP-test");piece.setManufacturerId("factory");
        piece.setQuantity(5);piece.setWidth(500D);piece.setHeight(400D);
        piece.setMaterialConfig(JSON.parseObject("{\"materialId\":\"material\",\"materialSnapshot\":{\"name\":\"material\"}}",MaterialConfig.class));
        var flow=new ProcedureFlow();var pending=new ProcedureFlowNode();pending.setNodeName("待排版");pending.setPieceQuantity(5);flow.setNodes(List.of(pending));piece.setProcedureFlow(flow);
        when(pieces.findById("piece")).thenReturn(piece);
        var cell=new TypesettingProductionPieceVO();cell.setSourceType(TypesettingSourceType.PART.getCode());cell.setSourceId("piece");cell.setQuantity(1);
        request=new LayoutConfirmRequest();request.setManufacturerMetaId("factory");request.setLayoutMode(TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_A30_LARGE_BOARD.getCode());
        request.setTypesettingCells(List.of(cell));request.setContainers(List.of(new LayoutConfirmRequest.ContainerInfo(1000,1000)));
    }
    @Test void rejectsOversizedPieceBeforeAnyWriteOrRemoteRequest() {
        piece.setWidth(3000D);piece.setHeight(3000D);
        var error=assertThrows(IllegalArgumentException.class,()->service.validateToLayoutRequest(request));
        assertTrue(error.getMessage().contains("PP-test零件的尺寸大于所选规格"));
        assertEquals(5,piece.getQuantity());assertEquals(1000,request.getContainers().getFirst().getWidth());
        verifyNoInteractions(algorithms,redis);
        verify(layouts,never()).updateTypesetting(any());
    }
    @Test void validAdmissionDoesNotChangeDatabaseSnapshotsOrClientPayload() {
        String before=JSON.toJSONString(request);
        service.validateToLayoutRequest(request);
        assertEquals(before,JSON.toJSONString(request));assertEquals(5,piece.getQuantity());
        verifyNoInteractions(algorithms,redis);
    }
    @Test void readOnlyCheckDoesNotApplyContainerInsetTwice() {
        var inset=mock(TypesettingContainerWidthInsetService.class);
        ReflectionTestUtils.setField(service,"containerWidthInsetService",inset);
        request.setLayoutMode(TypesettingLayoutMode.RECT_TYPESETTING_PLT_QR_CIRCLE.getCode());
        service.validateToLayoutRequest(request);service.validateToLayoutRequest(request);
        assertEquals(1000,request.getContainers().getFirst().getWidth());
    }
    @Test void rejectsWrongFactoryAndInsufficientQuantity() {
        request.setManufacturerMetaId("other");
        assertThrows(IllegalArgumentException.class,()->service.validateToLayoutRequest(request));
        request.setManufacturerMetaId("factory");request.getTypesettingCells().getFirst().setQuantity(6);
        assertTrue(assertThrows(IllegalArgumentException.class,()->service.validateToLayoutRequest(request)).getMessage().contains("数量不足"));
    }
    @Test void confirmationRejectsInvalidStateAndMissingSvgWithoutUpdatingState() {
        var info=new TypesettingInfo();info.setId("layout");info.setManufacturerMetaId("factory");info.setStatus(TypesettingStatus.CONFIRMED.getCode());
        when(layouts.findById("layout")).thenReturn(info);
        var input=new TypesettingInfo();input.setId("layout");
        assertThrows(IllegalArgumentException.class,()->service.validateConfirmLayoutRequest(input));
        info.setStatus(TypesettingStatus.CONFIRMING.getCode());
        assertTrue(assertThrows(IllegalArgumentException.class,()->service.validateConfirmLayoutRequest(input)).getMessage().contains("nestedSvg"));
        verify(layouts,never()).updateTypesetting(any());verifyNoInteractions(algorithms,redis);
    }
    @Test void printChecksDeviceBeforeBuildingGraphics() {
        var info=new TypesettingInfo();info.setId("layout");info.setManufacturerMetaId("factory");info.setStatus(TypesettingStatus.CONFIRMING.getCode());
        info.setLayoutMode(TypesettingLayoutMode.RECT_TYPESETTING_PLT_QR_CIRCLE.getCode());
        var element=new TypesettingElement();element.setNestedSvg("https://bucket/layout.svg");element.setWidth(BigDecimal.TEN);element.setHeight(BigDecimal.TEN);info.setElement(element);
        when(layouts.findById("layout")).thenReturn(info);
        @SuppressWarnings("unchecked") var builders=(Map<TypesettingLayoutMode,TypesettingLayoutModeBuildService>)ReflectionTestUtils.getField(service,"layoutModeBuildServiceMap");
        builders.put(TypesettingLayoutMode.RECT_TYPESETTING_PLT_QR_CIRCLE,mock(TypesettingLayoutModeBuildService.class));
        var devices=mock(com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerDeviceCfgRepository.class);
        ReflectionTestUtils.setField(service,"manufacturerDeviceCfgRepository",devices);
        var input=new ConfirmPrintRequest();input.setId("layout");input.setDeviceCode("missing");
        assertTrue(assertThrows(IllegalArgumentException.class,()->service.validateConfirmPrintRequest(input)).getMessage().contains("设备编号不存在"));
        verify(layouts,never()).updateTypesetting(any());verifyNoInteractions(algorithms,redis);
    }
}

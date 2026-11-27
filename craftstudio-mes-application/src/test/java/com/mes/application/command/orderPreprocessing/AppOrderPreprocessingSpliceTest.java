package com.mes.application.command.orderPreprocessing;

import com.mes.application.command.api.resp.ImageMaskResponse;
import com.mes.application.command.orderPreprocessing.splice.SpliceProcessStrategies;
import com.mes.application.command.typesetting.proces.buckle.FourCornerBuckleProcessService;
import com.mes.application.command.typesetting.proces.liubai.LiubaiProcessService;
import com.mes.application.command.typesetting.proces.splice.SuperWidthSpliceProcessService;
import com.mes.domain.manufacturer.procedure.service.ProcedureService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.service.ProcedureFlowService;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.service.TypesettingSequencePoolService;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.order.productionPieceGenerationTask.service.ProductionPieceGenerationTaskService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppOrderPreprocessingSpliceTest {
    @InjectMocks private AppOrderPreprocessingService service;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> redisValues;
    @Mock private OrderItemService orderItemService;
    @Mock private OrderInfoService orderInfoService;
    @Mock private ProcedureFlowService procedureFlowService;
    @Mock private ProcedureService procedureService;
    @Mock private ProductionPieceService productionPieceService;
    @Mock private ProductionPieceGenerationTaskService productionPieceGenerationTaskService;
    @Mock private TypesettingSequencePoolService typesettingSequencePoolService;
    @Mock private SuperWidthSpliceProcessService spliceProcessService;
    @Mock private LiubaiProcessService liubaiProcessService;
    @Mock private FourCornerBuckleProcessService buckleProcessService;

    @ParameterizedTest
    @ValueSource(strings = {"无痕拼接", "写真拼接", "板材拼接"})
    void zeroBleedSpliceCallbackAppliesLiubaiThenBuckleBeforeSaving(String spliceName) {
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(List.of(node(spliceName), node("留白15cm"), node("画外打扣"), node("四周打扣")));
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId("item");
        orderItem.setOrderId("order");
        orderItem.setManufacturerId("manufacturer");
        orderItem.setProcedureFlow(flow);

        ProductionPiece first = new ProductionPiece();
        ProductionPiece last = new ProductionPiece();
        when(redisTemplate.opsForValue()).thenReturn(redisValues);
        when(redisValues.setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.MINUTES))).thenReturn(true);
        when(orderItemService.findByOrderItemId("item")).thenReturn(orderItem);
        when(procedureFlowService.parseProcessingFlow(any(ProcedureFlow.class))).thenAnswer(call -> call.getArgument(0));
        when(procedureService.createProductionPiece(same(orderItem), eq("ORIGINAL"), anyString(),
                any(ProcedureFlow.class), anyString(), eq(800D), eq(600D))).thenReturn(first, last);
        when(productionPieceService.batchAddProductionPieces(List.of(first, last))).thenReturn(List.of(first, last));

        ImageMaskResponse response = new ImageMaskResponse();
        response.setPairs(List.of(part(1), part(2)));
        service.handleGenerateMaskFilesCallback(response, "item");

        InOrder steps = inOrder(spliceProcessService, liubaiProcessService, buckleProcessService, productionPieceService);
        for (ProductionPiece piece : List.of(first, last)) {
            steps.verify(spliceProcessService).process(same(orderItem), any(ProcedureFlow.class), same(piece), eq(piece.getBlood()));
            steps.verify(liubaiProcessService).process(argThat(context -> context.isSkipBloodEdges()
                    && context.getProductionPiece() == piece));
            steps.verify(buckleProcessService).process(same(orderItem), any(ProcedureFlow.class), same(piece));
            assertEquals(0, piece.getBlood().getX().intValue());
            assertEquals(0, piece.getBlood().getY().intValue());
        }
        steps.verify(productionPieceService).batchAddProductionPieces(List.of(first, last));
        assertEquals(0, SpliceProcessStrategies.findLastStrategy(flow).orElseThrow().bloodMm());
    }

    private ProcedureFlowNode node(String name) {
        ProcedureFlowNode node = new ProcedureFlowNode();
        node.setNodeName(name);
        return node;
    }

    private ImageMaskResponse.Pair part(int sequence) {
        ImageMaskResponse.Blood blood = new ImageMaskResponse.Blood();
        blood.setX(0);
        blood.setY(0);
        ImageMaskResponse.Pair result = new ImageMaskResponse.Pair();
        result.setImg("https://example.com/piece-" + sequence + ".jpg");
        result.setSvg("<svg width=\"800\" height=\"600\"></svg>");
        result.setGroup("group");
        result.setSeq(sequence);
        result.setBlood(blood);
        return result;
    }
}

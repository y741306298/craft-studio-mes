package com.mes.application.command.orderPreprocessing.strategy;

import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoubleSideMaskStrategyTest {

    @Test
    void processGeneratesRectMaskBeforeAsyncCallWhenSplicingAndDoubleSideWithoutSpecialShape() {
        DoubleSideMaskStrategy strategy = new DoubleSideMaskStrategy();
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId("order-item-1");
        ProcedureFlow procedureFlow = flow(
                node("背胶拼接", 1),
                node("覆双面(户内)", 2),
                node("反面不同画面", 3)
        );
        AppOrderPreprocessingService processingService = mock(AppOrderPreprocessingService.class);
        when(processingService.generateRectMaskSvgForStrategy(orderItem)).thenReturn("oss://mask/equal-width.svg");

        List<ProductionPiece> result = strategy.process(orderItem, procedureFlow, processingService);

        assertThat(result).isNull();
        InOrder inOrder = inOrder(processingService);
        inOrder.verify(processingService).generateRectMaskSvgForStrategy(orderItem);
        inOrder.verify(processingService).saveMaskToOrderItemForStrategy(orderItem, "oss://mask/equal-width.svg");
        inOrder.verify(processingService).callMaskAsyncForDoubleSide(orderItem, procedureFlow, "DOUBLE_SIDE", null);
    }

    @Test
    void processDoesNotGenerateRectMaskWhenSpecialShapeProvidesMaskForDoubleSideAsyncCall() {
        DoubleSideMaskStrategy strategy = new DoubleSideMaskStrategy();
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId("order-item-2");
        ProcedureFlow procedureFlow = flow(
                node("异形切割", 1),
                node("覆双面(户内)", 2)
        );
        AppOrderPreprocessingService processingService = mock(AppOrderPreprocessingService.class);

        List<ProductionPiece> result = strategy.process(orderItem, procedureFlow, processingService);

        assertThat(result).isNull();
        verify(processingService, never()).generateRectMaskSvgForStrategy(orderItem);
        verify(processingService, never()).saveMaskToOrderItemForStrategy(orderItem, any());
        verify(processingService).callMaskAsyncForDoubleSide(orderItem, procedureFlow, "DOUBLE_SIDE", null);
    }

    private ProcedureFlow flow(ProcedureFlowNode... nodes) {
        ProcedureFlow procedureFlow = new ProcedureFlow();
        procedureFlow.setNodes(List.of(nodes));
        return procedureFlow;
    }

    private ProcedureFlowNode node(String nodeName, Integer nodeOrder) {
        ProcedureFlowNode node = new ProcedureFlowNode();
        node.setNodeName(nodeName);
        node.setNodeOrder(nodeOrder);
        return node;
    }
}

package com.mes.application.command.orderPreprocessing;

import com.mes.application.command.orderPreprocessing.strategy.OrderItemProcessingStrategy;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.service.ProcedureFlowService;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.order.productionPieceGenerationTask.service.ProductionPieceGenerationTaskService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppOrderPreprocessingServiceTest {

    @Test
    void persistsDirectlyGeneratedPiecesOnceForTheWholePreprocessBatch() {
        AppOrderPreprocessingService service = new AppOrderPreprocessingService();
        OrderItemService orderItemService = mock(OrderItemService.class);
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        ProductionPieceGenerationTaskService generationTaskService = mock(ProductionPieceGenerationTaskService.class);
        ProcedureFlowService procedureFlowService = mock(ProcedureFlowService.class);
        OrderItemProcessingStrategy strategy = mock(OrderItemProcessingStrategy.class);
        OrderItem firstItem = orderItem("ORDER-1", "ITEM-1");
        OrderItem secondItem = orderItem("ORDER-1", "ITEM-2");
        ProductionPiece firstPiece = new ProductionPiece();
        ProductionPiece secondPiece = new ProductionPiece();

        when(procedureFlowService.parseProcessingFlow(any())).thenReturn(new ProcedureFlow());
        when(strategy.matches(any(), any())).thenReturn(true);
        when(strategy.process(any(), any(), any())).thenAnswer(invocation ->
                invocation.getArgument(0) == firstItem ? List.of(firstPiece) : List.of(secondPiece));
        when(productionPieceService.batchAddProductionPieces(any())).thenReturn(List.of(firstPiece, secondPiece));
        when(orderItemService.findByOrderItemId("ITEM-1")).thenReturn(firstItem);
        when(orderItemService.findByOrderItemId("ITEM-2")).thenReturn(secondItem);

        ReflectionTestUtils.setField(service, "orderItemService", orderItemService);
        ReflectionTestUtils.setField(service, "orderInfoService", orderInfoService);
        ReflectionTestUtils.setField(service, "productionPieceService", productionPieceService);
        ReflectionTestUtils.setField(service, "productionPieceGenerationTaskService", generationTaskService);
        ReflectionTestUtils.setField(service, "procedureFlowService", procedureFlowService);
        ReflectionTestUtils.setField(service, "orderItemProcessingStrategies", List.of(strategy));

        service.preprocessOrder(List.of(firstItem, secondItem));

        ArgumentCaptor<List<ProductionPiece>> piecesCaptor = ArgumentCaptor.forClass(List.class);
        verify(productionPieceService).batchAddProductionPieces(piecesCaptor.capture());
        assertThat(piecesCaptor.getValue()).containsExactly(firstPiece, secondPiece);
        verify(productionPieceService, never()).addProductionPiece(any());
        verify(generationTaskService).markGenerated("ORDER-1", "ITEM-1");
        verify(generationTaskService).markGenerated("ORDER-1", "ITEM-2");
    }

    private OrderItem orderItem(String orderId, String orderItemId) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setOrderItemId(orderItemId);
        item.setProcedureFlow(new ProcedureFlow());
        return item;
    }
}

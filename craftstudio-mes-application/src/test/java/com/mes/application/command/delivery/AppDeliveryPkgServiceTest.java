package com.mes.application.command.delivery;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.order.orderInfo.vo.OrderCustomer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AppDeliveryPkgServiceTest {

    private final AppDeliveryPkgService service = new AppDeliveryPkgService();

    @Test
    void usesDefaultRecipientNameWhenCustomerIsMissing() {
        assertThat(resolveRecipientName(null)).isEqualTo("神秘人");
    }

    @Test
    void usesDefaultRecipientNameWhenCustomerNameIsBlank() {
        OrderCustomer customer = new OrderCustomer();
        customer.setCustomerName(" ");

        assertThat(resolveRecipientName(customer)).isEqualTo("神秘人");
    }

    @Test
    void keepsProvidedCustomerName() {
        OrderCustomer customer = new OrderCustomer();
        customer.setCustomerName("张三");

        assertThat(resolveRecipientName(customer)).isEqualTo("张三");
    }

    @Test
    void doesNotCompleteRedoPieceWhenNonRedoPiecesAreFullyPacked() {
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        OrderItemService orderItemService = mock(OrderItemService.class);
        ReflectionTestUtils.setField(service, "productionPieceService", productionPieceService);
        ReflectionTestUtils.setField(service, "orderItemService", orderItemService);

        ProductionPiece nonRedoPiece = packedPiece("piece-1", false, 2);
        ProductionPiece redoPiece = packedPiece("redo-piece-1", true, 0);
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId("order-item-1");
        orderItem.setQuantity(2);
        when(productionPieceService.findByOrderItemIds(List.of("order-item-1")))
                .thenReturn(List.of(nonRedoPiece, redoPiece));
        when(orderItemService.findByOrderItemIds(List.of("order-item-1"))).thenReturn(List.of(orderItem));

        ReflectionTestUtils.invokeMethod(service, "refreshPackagingCompletionStatus", Set.of("order-item-1"));

        assertThat(nonRedoPiece.getStatus()).isEqualTo("completed");
        assertThat(redoPiece.getStatus()).isEqualTo(ProductionPieceStatus.PROCESSING.getCode());
        ArgumentCaptor<List<ProductionPiece>> changedPieces = ArgumentCaptor.forClass(List.class);
        verify(productionPieceService).batchUpdatePackagingState(changedPieces.capture());
        assertThat(changedPieces.getValue()).containsExactly(nonRedoPiece);
    }

    @Test
    void redoPieceOnlyUpdatesItsOwnPackagingState() {
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        OrderItemService orderItemService = mock(OrderItemService.class);
        ReflectionTestUtils.setField(service, "productionPieceService", productionPieceService);
        ReflectionTestUtils.setField(service, "orderItemService", orderItemService);

        ProductionPiece redoPiece = packedPiece("redo-piece-1", true, 2);
        Set<String> touchedOrderItemIds = new java.util.HashSet<>();

        ReflectionTestUtils.invokeMethod(service, "updatePiecePackagingStateAfterTransfer", redoPiece,
                touchedOrderItemIds);
        ReflectionTestUtils.invokeMethod(service, "refreshPackagingCompletionStatus", touchedOrderItemIds);

        assertThat(redoPiece.getStatus()).isEqualTo("completed");
        assertThat(touchedOrderItemIds).isEmpty();
        verifyNoInteractions(productionPieceService, orderItemService);
    }

    private ProductionPiece packedPiece(String id, boolean redo, int packedQuantity) {
        ProcedureFlowNode packedNode = new ProcedureFlowNode();
        packedNode.setNodeId("NODE_PACKAGED");
        packedNode.setNodeName("已打包");
        packedNode.setPieceQuantity(packedQuantity);
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(List.of(packedNode));

        ProductionPiece piece = new ProductionPiece();
        piece.setId(id);
        piece.setOrderItemId("order-item-1");
        piece.setStatus(ProductionPieceStatus.PROCESSING.getCode());
        piece.setIsRedo(redo);
        piece.setProcedureFlow(flow);
        return piece;
    }

    private String resolveRecipientName(OrderCustomer customer) {
        return ReflectionTestUtils.invokeMethod(service, "resolveRecipientName", customer);
    }
}

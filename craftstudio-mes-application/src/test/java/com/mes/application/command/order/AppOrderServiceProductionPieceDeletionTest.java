package com.mes.application.command.order;

import com.mes.application.command.order.vo.OrderProductionPieceDeletionResult;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppOrderServiceProductionPieceDeletionTest {

    @Test
    void findsOrderItemsAndSoftDeletesProductionPiecesInOneBatch() {
        OrderItemService orderItemService = mock(OrderItemService.class);
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        AppOrderService service = service(orderItemService, productionPieceService);
        List<String> orderItemIds = List.of("item-1", "item-2");

        when(orderItemService.findAllByOrderId("order-1")).thenReturn(List.of(
                orderItem("item-1"), orderItem("item-2"), orderItem("item-1")));
        when(productionPieceService.deleteProductionPiecesByOrderItemIds(orderItemIds)).thenReturn(3L);

        OrderProductionPieceDeletionResult result = service.deleteProductionPiecesByOrderId(" order-1 ");

        assertThat(result.getOrderId()).isEqualTo("order-1");
        assertThat(result.getOrderItemIds()).containsExactly("item-1", "item-2");
        assertThat(result.getDeletedProductionPieceCount()).isEqualTo(3);
        verify(productionPieceService).deleteProductionPiecesByOrderItemIds(orderItemIds);
        verify(productionPieceService, never()).deleteProductionPiecesByOrderItemId(
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsAnOrderWithoutOrderItemsWithoutDeletingPieces() {
        OrderItemService orderItemService = mock(OrderItemService.class);
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        AppOrderService service = service(orderItemService, productionPieceService);
        when(orderItemService.findAllByOrderId("missing-order")).thenReturn(List.of());

        assertThatThrownBy(() -> service.deleteProductionPiecesByOrderId("missing-order"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("订单项不存在，orderId：missing-order");

        verify(productionPieceService, never())
                .deleteProductionPiecesByOrderItemIds(org.mockito.ArgumentMatchers.anyCollection());
    }

    private AppOrderService service(OrderItemService orderItemService,
                                    ProductionPieceService productionPieceService) {
        AppOrderService service = new AppOrderService();
        ReflectionTestUtils.setField(service, "domainOrderItemService", orderItemService);
        ReflectionTestUtils.setField(service, "productionPieceService", productionPieceService);
        return service;
    }

    private OrderItem orderItem(String orderItemId) {
        OrderItem item = new OrderItem();
        item.setOrderItemId(orderItemId);
        return item;
    }
}

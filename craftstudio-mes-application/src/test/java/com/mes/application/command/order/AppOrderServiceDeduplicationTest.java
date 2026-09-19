package com.mes.application.command.order;

import com.mes.application.command.order.vo.OrderItemDeduplicationResult;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppOrderServiceDeduplicationTest {

    private static final String ORDER_ID = "2101141077316124674";

    @Test
    void keepsEarliestOrderItemAndDeletesLaterItemsAndTheirProductionPieces() {
        OrderItemService orderItemService = mock(OrderItemService.class);
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        AppOrderService service = service(orderItemService, productionPieceService);

        OrderItem earliest = orderItem("mongo-1", "item-1", 1000);
        OrderItem latest = orderItem("mongo-3", "item-3", 3000);
        OrderItem middle = orderItem("mongo-2", "item-2", 2000);
        when(orderItemService.filterList(1, 100, Map.of("orderId", ORDER_ID)))
                .thenReturn(List.of(latest, earliest, middle));
        when(productionPieceService.deleteProductionPiecesByOrderItemId("item-2")).thenReturn(2L);
        when(productionPieceService.deleteProductionPiecesByOrderItemId("item-3")).thenReturn(1L);
        OrderItemDeduplicationResult result = service.deduplicateOrderItems("  " + ORDER_ID + "  ");

        assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(result.getKeptOrderItemId()).isEqualTo("item-1");
        assertThat(result.getMatchedOrderItemCount()).isEqualTo(3);
        assertThat(result.getDeletedOrderItemCount()).isEqualTo(2);
        assertThat(result.getDeletedProductionPieceCount()).isEqualTo(3);
        verify(orderItemService, never()).deleteOrderItem("mongo-1");
        var deletionOrder = inOrder(productionPieceService, orderItemService);
        deletionOrder.verify(productionPieceService).deleteProductionPiecesByOrderItemId("item-2");
        deletionOrder.verify(orderItemService).deleteOrderItem("mongo-2");
        deletionOrder.verify(productionPieceService).deleteProductionPiecesByOrderItemId("item-3");
        deletionOrder.verify(orderItemService).deleteOrderItem("mongo-3");
    }

    @Test
    void rejectsMissingOrderIdWithoutDeletingAnything() {
        OrderItemService orderItemService = mock(OrderItemService.class);
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        AppOrderService service = service(orderItemService, productionPieceService);

        assertThatThrownBy(() -> service.deduplicateOrderItems(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("订单 ID 不能为空");

        verify(orderItemService, never()).deleteOrderItem(org.mockito.ArgumentMatchers.anyString());
        verify(productionPieceService, never())
                .deleteProductionPiecesByOrderItemId(org.mockito.ArgumentMatchers.anyString());
    }

    private AppOrderService service(OrderItemService orderItemService,
                                    ProductionPieceService productionPieceService) {
        AppOrderService service = new AppOrderService();
        ReflectionTestUtils.setField(service, "domainOrderItemService", orderItemService);
        ReflectionTestUtils.setField(service, "productionPieceService", productionPieceService);
        return service;
    }

    private OrderItem orderItem(String id, String orderItemId, long createTime) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setOrderId(ORDER_ID);
        item.setOrderItemId(orderItemId);
        item.setCreateTime(new Date(createTime));
        return item;
    }
}

package com.mes.application.command.delivery;

import com.mes.application.dto.req.delivery.DeliveryPkgScopedRequest;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppDeliveryPkgServiceBatchQueryTest {

    @Test
    void queriesProductionPiecesOnceForAllOrderItems() {
        AppDeliveryPkgService service = new AppDeliveryPkgService();
        OrderItemService orderItemService = mock(OrderItemService.class);
        ProductionPieceService productionPieceService = mock(ProductionPieceService.class);
        ReflectionTestUtils.setField(service, "orderItemService", orderItemService);
        ReflectionTestUtils.setField(service, "productionPieceService", productionPieceService);

        OrderItem first = orderItem("item-1");
        OrderItem second = orderItem("item-2");
        when(orderItemService.findByOrderId("order-1", "manufacturer-1", 1, 100))
                .thenReturn(List.of(first, second));
        when(productionPieceService.findPendingPackagingPiecesByOrderItemIds(
                eq("manufacturer-1"), anyCollection(), eq("acrylic")))
                .thenReturn(Collections.emptyList());

        DeliveryPkgScopedRequest request = new DeliveryPkgScopedRequest();
        request.setManufacturerMetaId("manufacturer-1");
        request.setOrderId("order-1");
        request.setMaterialName("acrylic");

        service.listPendingPackagingPiecesById(request);

        verify(productionPieceService, times(1)).findPendingPackagingPiecesByOrderItemIds(
                eq("manufacturer-1"),
                argThat(ids -> ids.equals(new LinkedHashSet<>(List.of("item-1", "item-2")))),
                eq("acrylic"));
    }

    private OrderItem orderItem(String id) {
        OrderItem item = new OrderItem();
        item.setOrderItemId(id);
        return item;
    }
}

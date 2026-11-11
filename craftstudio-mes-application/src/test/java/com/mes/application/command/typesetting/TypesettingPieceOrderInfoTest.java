package com.mes.application.command.typesetting;

import com.mes.application.command.typesetting.vo.TypesettingProductionPieceVO;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TypesettingPieceOrderInfoTest {
    @Test
    void loadsOrdersInBatchesForManyPiecesAndSupportsLegacyItemIds() {
        AppTypesettingService service = new AppTypesettingService();
        OrderItemService itemService = mock(OrderItemService.class);
        OrderInfoService orderService = mock(OrderInfoService.class);
        ReflectionTestUtils.setField(service, "orderItemService", itemService);
        ReflectionTestUtils.setField(service, "orderInfoService", orderService);
        OrderItem item = new OrderItem();
        item.setId("mongo-item");
        item.setOrderItemId("item");
        item.setOrderId("order");
        OrderInfo order = new OrderInfo();
        order.setOrderId("order");
        List<ProductionPiece> pieces = IntStream.range(0, 100).mapToObj(i -> {
            ProductionPiece piece = new ProductionPiece();
            piece.setOrderItemId(i % 2 == 0 ? "item" : "mongo-item");
            return piece;
        }).toList();
        when(itemService.findByOrderItemIds(Set.of("item", "mongo-item"))).thenReturn(List.of(item));
        when(orderService.findByOrderIds(Set.of("order"))).thenReturn(List.of(order));

        Map<String, OrderInfo> result = ReflectionTestUtils.invokeMethod(service, "loadOrdersForPieces", pieces);

        assertSame(order, result.get("item"));
        assertSame(order, result.get("mongo-item"));
        verify(itemService, times(1)).findByOrderItemIds(Set.of("item", "mongo-item"));
        verify(orderService, times(1)).findByOrderIds(Set.of("order"));
        verifyNoMoreInteractions(itemService, orderService);
    }

    @Test
    void returnsBusinessPieceIdSeparatelyFromMongoId() {
        ProductionPiece piece = new ProductionPiece();
        piece.setId("mongo-piece");
        piece.setProductionPieceId("business-piece");
        TypesettingProductionPieceVO result = TypesettingProductionPieceVO.fromProductionPiece(piece);
        assertEquals("business-piece", result.getProductionPieceId());
        assertEquals("mongo-piece", result.getId());
        assertEquals("mongo-piece", result.getSourceId());
    }
}

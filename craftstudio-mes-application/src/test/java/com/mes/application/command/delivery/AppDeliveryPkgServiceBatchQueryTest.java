package com.mes.application.command.delivery;

import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import com.mes.application.dto.req.delivery.DeliveryPkgAddRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgScopedRequest;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void putsOriginalOrderRemarkBetweenOrderIdAndFileName() {
        AppDeliveryPkgService service = new AppDeliveryPkgService();
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        OrderItemService orderItemService = mock(OrderItemService.class);
        ReflectionTestUtils.setField(service, "orderInfoService", orderInfoService);
        ReflectionTestUtils.setField(service, "orderItemService", orderItemService);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setRemark("原备注");
        when(orderInfoService.findByOrderId("order-1")).thenReturn(orderInfo);

        OrderItem orderItem = orderItem("item-1");
        ImageFile productionImage = new ImageFile();
        productionImage.setName("production.png");
        orderItem.setProductionImgFile(productionImage);
        when(orderItemService.findByOrderItemId("item-1")).thenReturn(orderItem);

        DeliveryPkgPieceVO piece = new DeliveryPkgPieceVO();
        piece.setOrderItemId("item-1");
        DeliveryPkgAddRequest.DeliveryPkgPieceItem pieceItem = new DeliveryPkgAddRequest.DeliveryPkgPieceItem();
        pieceItem.setPiece(piece);

        String remarks = ReflectionTestUtils.invokeMethod(service, "buildDeliveryPkgRemarks",
                "order-1", "CUSTOM", null, List.of(pieceItem), null);

        assertEquals("订单:order-1\n原备注\n文件:production.png", remarks);
    }

    private OrderItem orderItem(String id) {
        OrderItem item = new OrderItem();
        item.setOrderItemId(id);
        return item;
    }
}

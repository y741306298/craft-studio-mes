package com.mes.application.command.order;

import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppOrderServiceTest {

    @Mock
    private OrderInfoService domainOrderInfoService;

    @Mock
    private OrderItemService domainOrderItemService;

    @Mock
    private ProductionPieceService productionPieceService;

    @InjectMocks
    private AppOrderService appOrderService;

    @Test
    void toggleOrderItemUrgentUpdatesProductionPiecesByBusinessOrderItemId() {
        String mongoId = "mongo-order-item-id";
        String businessOrderItemId = "OI202606050001";
        OrderItem orderItem = new OrderItem();
        orderItem.setId(mongoId);
        orderItem.setOrderItemId(businessOrderItemId);
        orderItem.setIsUrgent(false);
        when(domainOrderItemService.findById(mongoId)).thenReturn(orderItem);

        appOrderService.toggleOrderItemUrgent(mongoId);

        verify(domainOrderItemService).updateOrderItem(orderItem);
        verify(productionPieceService).updateUrgentByOrderItemId(businessOrderItemId, true);
    }
}

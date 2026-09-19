package com.mes.domain.order.orderInfo.service;

import com.mes.domain.delivery.deliveryRoute.repository.AddressRecognitionRecordRepository;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.repository.OrderInfoRepository;
import com.mes.domain.order.orderInfo.repository.OrderItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderInfoServiceBatchDeduplicationTest {

    @Test
    void checksExistingOrderItemsWithOneBatchQuery() {
        OrderInfoRepository orderInfoRepository = mock(OrderInfoRepository.class);
        OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
        OrderInfoService service = new OrderInfoService();
        ReflectionTestUtils.setField(service, "orderInfoRepository", orderInfoRepository);
        ReflectionTestUtils.setField(service, "orderItemRepository", orderItemRepository);
        ReflectionTestUtils.setField(service, "orderPreprocessingService", mock(OrderPreprocessingService.class));
        ReflectionTestUtils.setField(service, "addressRecognitionRecordRepository",
                mock(AddressRecognitionRecordRepository.class));

        OrderInfo order = new OrderInfo();
        order.setOrderId("order-1");
        order.setManufacturerId("manufacturer-1");
        OrderItem existing = orderItem("item-1");
        OrderItem newItem = orderItem("item-2");
        OrderItem duplicateInRequest = orderItem("item-2");
        when(orderItemRepository.findByOrderIdAndOrderItemIds(
                "order-1", Set.of("item-1", "item-2"))).thenReturn(List.of(existing));
        when(orderInfoRepository.filterList(1, 1, java.util.Map.of(
                "orderId", "order-1", "manufacturerId", "manufacturer-1"))).thenReturn(List.of());
        when(orderItemRepository.batchAdd(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<OrderItem> result = service.addOrderWithItems(
                order, List.of(existing, newItem, duplicateInRequest));

        assertThat(result).extracting(OrderItem::getOrderItemId).containsExactly("item-2");
        verify(orderItemRepository).findByOrderIdAndOrderItemIds(
                "order-1", Set.of("item-1", "item-2"));
        verify(orderItemRepository, never()).filterTotal(org.mockito.ArgumentMatchers.anyMap());
    }

    private OrderItem orderItem(String orderItemId) {
        OrderItem item = new OrderItem();
        item.setOrderItemId(orderItemId);
        item.setManufacturerId("manufacturer-1");
        return item;
    }
}

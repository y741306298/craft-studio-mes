package com.mes.application.command.order;

import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.infra.mq.LogisticsOrderProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppPreOrderLabelTaskServiceMqRetryTest {
    private AppPreOrderLabelTaskService service;
    private OrderInfoService orderInfoService;
    private LogisticsOrderProducer producer;

    @BeforeEach
    void setUp() {
        service = new AppPreOrderLabelTaskService();
        orderInfoService = mock(OrderInfoService.class);
        producer = mock(LogisticsOrderProducer.class);
        ObjectProvider<RocketMQTemplate> templateProvider = mock(ObjectProvider.class);
        when(templateProvider.getIfAvailable()).thenReturn(mock(RocketMQTemplate.class));
        ReflectionTestUtils.setField(service, "orderInfoService", orderInfoService);
        ReflectionTestUtils.setField(service, "producer", producer);
        ReflectionTestUtils.setField(service, "rocketMQTemplateProvider", templateProvider);
    }

    @Test
    void retriesOnlySuccessfullyPrePrintedNonReturnedOrders() {
        Date start = new Date(1_000);
        Date end = new Date(2_000);
        OrderInfo ready = order("10001", "SF123", OrderStatus.PENDING);
        OrderInfo notPrinted = order("10002", null, OrderStatus.PENDING);
        OrderInfo returned = order("10003", "SF456", OrderStatus.RETURNED);
        when(orderInfoService.findOrdersByConditions(null, null, start, end, null, 1, 100))
                .thenReturn(List.of(ready, notPrinted, returned));

        AppPreOrderLabelTaskService.LogisticsMqRetryResult result =
                service.retryLogisticsMqNotifications(start, end);

        assertEquals(3, result.getScannedCount());
        assertEquals(2, result.getSkippedCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        verify(producer).send(any());
    }

    @Test
    void doesNotSendWhenNoOrderHasAStoredWaybillNumber() {
        Date start = new Date(1_000);
        Date end = new Date(2_000);
        when(orderInfoService.findOrdersByConditions(null, null, start, end, null, 1, 100))
                .thenReturn(List.of(order("10001", " ", OrderStatus.PENDING)));

        AppPreOrderLabelTaskService.LogisticsMqRetryResult result =
                service.retryLogisticsMqNotifications(start, end);

        assertEquals(1, result.getSkippedCount());
        assertEquals(0, result.getSuccessCount());
        verify(producer, never()).send(any());
    }

    private OrderInfo order(String orderId, String kuaidiNum, OrderStatus status) {
        OrderInfo order = new OrderInfo();
        order.setOrderId(orderId);
        order.setPlatformCode("TEST");
        order.setKuaidiNum(kuaidiNum);
        order.setStatus(status);
        return order;
    }
}

package com.mes.application.command.order;

import com.mes.application.command.orderPreprocessing.OrderPreprocessTaskQueue;
import com.mes.application.dto.req.order.OrderAddRequest;
import com.mes.application.support.PodvOrgInfoHelper;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderItemPriceAllocation.repository.OrderItemPriceAllocationRepository;
import com.mes.domain.order.orderStatistics.service.OrderDailyStatisticsService;
import com.mes.domain.order.preOrderLabelTask.service.PreOrderLabelTaskService;
import com.mes.domain.order.productionPieceGenerationTask.service.ProductionPieceGenerationTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppOrderServiceAddPerformanceTest {

    @Test
    void enqueuesPreprocessingImmediatelyAfterPersistence() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        ProductionPieceGenerationTaskService generationTaskService = mock(ProductionPieceGenerationTaskService.class);
        PreOrderLabelTaskService labelTaskService = mock(PreOrderLabelTaskService.class);
        OrderPreprocessTaskQueue taskQueue = mock(OrderPreprocessTaskQueue.class);
        OrderItemPriceAllocationRepository allocationRepository = mock(OrderItemPriceAllocationRepository.class);
        OrderDailyStatisticsService statisticsService = mock(OrderDailyStatisticsService.class);
        PodvOrgInfoHelper orgInfoHelper = mock(PodvOrgInfoHelper.class);
        AppOrderService service = new AppOrderService();
        ReflectionTestUtils.setField(service, "domainOrderInfoService", orderInfoService);
        ReflectionTestUtils.setField(service, "productionPieceGenerationTaskService", generationTaskService);
        ReflectionTestUtils.setField(service, "preOrderLabelTaskService", labelTaskService);
        ReflectionTestUtils.setField(service, "orderPreprocessTaskQueue", taskQueue);
        ReflectionTestUtils.setField(service, "orderItemPriceAllocationRepository", allocationRepository);
        ReflectionTestUtils.setField(service, "orderDailyStatisticsService", statisticsService);
        ReflectionTestUtils.setField(service, "podvOrgInfoHelper", orgInfoHelper);

        OrderAddRequest request = mock(OrderAddRequest.class);
        OrderInfo order = new OrderInfo();
        order.setOrderId("order-1");
        order.setManufacturerId("manufacturer-1");
        OrderItem item = new OrderItem();
        item.setOrderItemId("item-1");
        item.setManufacturerId("manufacturer-1");
        when(request.toOrderInfo()).thenReturn(order);
        when(request.toOrderItems()).thenReturn(List.of(item));
        when(orderInfoService.addOrderWithItems(order, List.of(item))).thenReturn(List.of(item));

        service.addOrderWithItems(request);

        verify(taskQueue).submit(List.of(item));
        verify(allocationRepository).batchAdd(List.of());
    }
}

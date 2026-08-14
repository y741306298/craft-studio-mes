package com.mes.application.command.order;

import com.mes.application.command.statistics.vo.OrderStatisticsListVO;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteRepository;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.order.orderStatistics.service.OrderDailyStatisticsService;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppOrderServiceStatisticsRouteTest {

    @Test
    void loadsRouteNamesWithOneBatchQuery() {
        OrderItemService orderItemService = mock(OrderItemService.class);
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        DeliveryRouteRepository routeRepository = mock(DeliveryRouteRepository.class);
        OrderDailyStatisticsService statisticsService = mock(OrderDailyStatisticsService.class);
        AppOrderService service = new AppOrderService();
        ReflectionTestUtils.setField(service, "domainOrderItemService", orderItemService);
        ReflectionTestUtils.setField(service, "domainOrderInfoService", orderInfoService);
        ReflectionTestUtils.setField(service, "deliveryRouteRepository", routeRepository);
        ReflectionTestUtils.setField(service, "orderDailyStatisticsService", statisticsService);

        OrderItem first = orderItem("ORDER_1", "ROUTE_1");
        OrderItem second = orderItem("ORDER_2", "ROUTE_1");
        DeliveryRoute route = new DeliveryRoute();
        route.setRouteId("ROUTE_1");
        route.setRouteName("城区路线");
        when(orderItemService.filterListUrgentFirst(1, 20, java.util.Map.of("manufacturerId", "M_1")))
                .thenReturn(List.of(first, second));
        when(orderItemService.filterTotal(any())).thenReturn(2L);
        when(orderInfoService.findByOrderIds(any())).thenReturn(List.of());
        when(routeRepository.findByRouteIds(any())).thenReturn(List.of(route));

        OrderStatisticsListVO result = service.findOrderStatistics("M_1", null, null, null, null,
                null, null, null, null, null, new PagedQuery(1, 20));

        assertEquals(List.of("城区路线", "城区路线"), result.getItems().stream()
                .map(item -> item.getRouteName()).toList());
        verify(routeRepository).findByRouteIds(org.mockito.ArgumentMatchers.argThat(
                (Collection<String> ids) -> ids.size() == 1 && ids.contains("ROUTE_1")));
        verify(routeRepository, never()).findByRouteId(any());
    }

    private OrderItem orderItem(String orderId, String routeId) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setRouteId(routeId);
        return item;
    }
}

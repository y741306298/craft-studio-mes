package com.mes.application.command.order;

import com.mes.application.command.order.vo.OrderPriceStatisticsVO;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.vo.ManufacturerInfo;
import com.mes.domain.order.orderInfo.vo.OrderPriceInfo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppOrderServicePriceStatisticsTest {

    @Test
    void shouldReturnAllOrdersButExcludeReturnedOrdersFromTotals() {
        AppOrderService service = new AppOrderService();
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        ReflectionTestUtils.setField(service, "domainOrderInfoService", orderInfoService);
        Date startTime = new Date(1_000);
        Date endTime = new Date(2_000);
        OrderInfo normalOrder = order(OrderStatus.PACKAGED, "10.20", "2.30", "12.50");
        OrderInfo returnedOrder = order(OrderStatus.RETURNED, "100", "20", "120");
        OrderInfo emptyPriceOrder = new OrderInfo();
        emptyPriceOrder.setStatus(OrderStatus.PENDING);
        when(orderInfoService.findOrdersByManufacturerAndCreateTime("factory-1", startTime, endTime, 1, 100))
                .thenReturn(List.of(normalOrder, returnedOrder, emptyPriceOrder));

        OrderPriceStatisticsVO result = service.findOrderPriceStatistics("factory-1", startTime, endTime);

        assertThat(result.getOrderInfos()).containsExactly(normalOrder, returnedOrder, emptyPriceOrder);
        assertThat(result.getActualPriceTotal()).isEqualByComparingTo("10.20");
        assertThat(result.getLogisticsPriceTotal()).isEqualByComparingTo("2.30");
        assertThat(result.getPaymentPriceTotal()).isEqualByComparingTo("12.50");
    }

    private OrderInfo order(OrderStatus status, String actualPrice, String logisticsPrice, String paymentPrice) {
        OrderPriceInfo price = new OrderPriceInfo();
        price.setActualPrice(new BigDecimal(actualPrice));
        price.setLogisticsPrice(new BigDecimal(logisticsPrice));
        price.setPaymentPrice(new BigDecimal(paymentPrice));
        ManufacturerInfo manufacturerInfo = new ManufacturerInfo();
        manufacturerInfo.setPrice(price);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(status);
        orderInfo.setManufacturerInfo(manufacturerInfo);
        return orderInfo;
    }
}

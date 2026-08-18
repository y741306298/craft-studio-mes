package com.mes.application.command.order;

import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.vo.ManufacturerInfo;
import com.mes.domain.order.orderInfo.vo.OrderPriceInfo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AppOrderServiceStatisticsTest {

    private final AppOrderService service = new AppOrderService();

    @Test
    void shouldUseManufacturerActualPriceForManufacturerStatistics() {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setPrice(price("100.00", "108.00"));
        ManufacturerInfo manufacturerInfo = new ManufacturerInfo();
        manufacturerInfo.setPrice(price("5.00", "13.00"));
        orderInfo.setManufacturerInfo(manufacturerInfo);

        assertThat(service.resolveManufacturerActualPrice(orderInfo))
                .isEqualByComparingTo("5.00");
    }

    @Test
    void shouldFallBackToOrderActualPriceForLegacyOrders() {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setPrice(price("100.00", "108.00"));

        assertThat(service.resolveManufacturerActualPrice(orderInfo))
                .isEqualByComparingTo("100.00");
    }

    private OrderPriceInfo price(String actualPrice, String paymentPrice) {
        OrderPriceInfo price = new OrderPriceInfo();
        price.setActualPrice(new BigDecimal(actualPrice));
        price.setPaymentPrice(new BigDecimal(paymentPrice));
        return price;
    }
}

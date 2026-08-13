package com.mes.application.command.order;

import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.vo.ManufacturerInfo;
import com.mes.domain.order.orderInfo.vo.OrderPriceInfo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppOrderServiceStatisticsAmountTest {

    private final AppOrderService service = new AppOrderService();

    @Test
    void sumsFloorPricesWhenManifestIsComplete() {
        OrderInfo orderInfo = orderWithManufacturerPrice("13");
        orderInfo.getManufacturerInfo().setFloorPriceEffectManifest(manifest(item("1"), item("0.12")));

        assertEquals(new BigDecimal("1.12"), calculateStatisticsAmount(orderInfo));
    }

    @Test
    void usesManufacturerPaymentPriceWhenManifestIsEmpty() {
        OrderInfo orderInfo = orderWithManufacturerPrice("13");
        orderInfo.getManufacturerInfo().setFloorPriceEffectManifest(manifest());

        assertEquals(new BigDecimal("13"), calculateStatisticsAmount(orderInfo));
    }

    @Test
    void usesManufacturerPaymentPriceWhenAnyFloorPriceIsMissing() {
        OrderInfo orderInfo = orderWithManufacturerPrice("13");
        orderInfo.getManufacturerInfo().setFloorPriceEffectManifest(manifest(item("1"), item(null)));

        assertEquals(new BigDecimal("13"), calculateStatisticsAmount(orderInfo));
    }

    @Test
    void usesOrderPaymentPriceForOrdersWithoutManufacturerPricingSnapshot() {
        OrderInfo orderInfo = new OrderInfo();
        OrderPriceInfo price = new OrderPriceInfo();
        price.setPaymentPrice(new BigDecimal("9.08"));
        orderInfo.setPrice(price);

        assertEquals(new BigDecimal("9.08"), calculateStatisticsAmount(orderInfo));
    }

    private OrderInfo orderWithManufacturerPrice(String paymentPrice) {
        OrderPriceInfo price = new OrderPriceInfo();
        price.setPaymentPrice(new BigDecimal(paymentPrice));
        ManufacturerInfo manufacturerInfo = new ManufacturerInfo();
        manufacturerInfo.setPrice(price);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setManufacturerInfo(manufacturerInfo);
        return orderInfo;
    }

    private ManufacturerInfo.FloorPriceEffectManifest manifest(ManufacturerInfo.FloorPriceEffectItem... items) {
        ManufacturerInfo.FloorPriceEffectManifest manifest = new ManufacturerInfo.FloorPriceEffectManifest();
        manifest.setFloorPriceEffectItems(List.of(items));
        return manifest;
    }

    private ManufacturerInfo.FloorPriceEffectItem item(String floorPrice) {
        ManufacturerInfo.FloorPriceEffectItem item = new ManufacturerInfo.FloorPriceEffectItem();
        item.setFloorPrice(floorPrice == null ? null : new BigDecimal(floorPrice));
        return item;
    }

    private BigDecimal calculateStatisticsAmount(OrderInfo orderInfo) {
        return service.calculateStatisticsAmount(orderInfo);
    }
}

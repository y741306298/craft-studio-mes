package com.mes.application.command.order;

import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.vo.ManufacturerInfo;
import com.mes.domain.order.orderInfo.vo.OrderItemPriceInfo;
import com.mes.domain.order.orderInfo.vo.OrderPriceInfo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppOrderServiceStatisticsAmountTest {

    private final AppOrderService service = new AppOrderService();

    @Test
    void sumsFloorPricesFromOrderItemWhenManifestIsComplete() {
        OrderItem orderItem = itemWithManufacturerPrice("13");
        orderItem.getManufacturerInfo().setFloorPriceEffectManifest(manifest(item("1"), item("0.12")));

        assertEquals(new BigDecimal("1.12"), calculateStatisticsAmount(orderItem));
    }

    @Test
    void usesOrderItemManufacturerPaymentPriceWhenManifestIsEmpty() {
        OrderItem orderItem = itemWithManufacturerPrice("13");
        orderItem.getManufacturerInfo().setFloorPriceEffectManifest(manifest());

        assertEquals(new BigDecimal("13"), calculateStatisticsAmount(orderItem));
    }

    @Test
    void usesOrderItemManufacturerPaymentPriceWhenAnyFloorPriceIsMissing() {
        OrderItem orderItem = itemWithManufacturerPrice("13");
        orderItem.getManufacturerInfo().setFloorPriceEffectManifest(manifest(item("1"), item(null)));

        assertEquals(new BigDecimal("13"), calculateStatisticsAmount(orderItem));
    }

    @Test
    void usesOrderItemActualPriceWithoutManufacturerPricingSnapshot() {
        OrderItem orderItem = new OrderItem();
        OrderItemPriceInfo price = new OrderItemPriceInfo();
        price.setActualPrice(new BigDecimal("1.08"));
        orderItem.setPrice(price);

        assertEquals(new BigDecimal("1.08"), calculateStatisticsAmount(orderItem));
    }

    private OrderItem itemWithManufacturerPrice(String paymentPrice) {
        OrderPriceInfo price = new OrderPriceInfo();
        price.setPaymentPrice(new BigDecimal(paymentPrice));
        ManufacturerInfo manufacturerInfo = new ManufacturerInfo();
        manufacturerInfo.setPrice(price);
        OrderItem orderItem = new OrderItem();
        orderItem.setManufacturerInfo(manufacturerInfo);
        return orderItem;
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

    private BigDecimal calculateStatisticsAmount(OrderItem orderItem) {
        return service.calculateStatisticsAmount(orderItem);
    }
}

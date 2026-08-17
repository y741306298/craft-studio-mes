package com.mes.application.command.order;

import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.vo.ManufacturerInfo;
import com.mes.domain.order.orderInfo.vo.OrderItemPriceInfo;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppOrderServiceStatisticsAmountTest {

    private final AppOrderService service = new AppOrderService();

    @Test
    void countsOneOrderLevelFloorPriceForItemsOfTheSameMaterial() {
        OrderInfo order = orderWithFloorPrices(floor("MATERIAL_A", "8"));
        OrderItem first = orderItem("MATERIAL_A", "5");
        OrderItem second = orderItem("MATERIAL_A", "6");

        assertEquals(new BigDecimal("8"), service.calculateStatisticsAmount(order, List.of(first, second)));
    }

    @Test
    void countsDifferentFloorPriceMaterialReferencesOnceAndAddsUnmatchedItems() {
        OrderInfo order = orderWithFloorPrices(floor("MATERIAL_A", "8"), floor("MATERIAL_B", "3"));
        List<OrderItem> items = List.of(
                orderItem("MATERIAL_A", "5"),
                orderItem("MATERIAL_A", "6"),
                orderItem("MATERIAL_C", "2.50"));

        assertEquals(new BigDecimal("13.50"), service.calculateStatisticsAmount(order, items));
    }

    @Test
    void usesActualPriceWhenItemMaterialHasNoFloorPrice() {
        OrderInfo order = orderWithFloorPrices(floor("MATERIAL_A", "8"));
        OrderItem item = orderItem("MATERIAL_B", "5.55");

        assertEquals(new BigDecimal("5.55"), service.calculateStatisticsAmount(order, List.of(item)));
    }

    @Test
    void ignoresFloorEntryWithoutPriceForItemCalculation() {
        OrderInfo order = orderWithFloorPrices(floor("MATERIAL_A", null));
        OrderItem item = orderItem("MATERIAL_A", "5.55");

        assertEquals(new BigDecimal("5.55"), service.calculateStatisticsAmount(order, List.of(item)));
    }

    private OrderInfo orderWithFloorPrices(ManufacturerInfo.FloorPriceEffectItem... items) {
        ManufacturerInfo.FloorPriceEffectManifest manifest = new ManufacturerInfo.FloorPriceEffectManifest();
        manifest.setFloorPriceEffectItems(List.of(items));
        ManufacturerInfo manufacturerInfo = new ManufacturerInfo();
        manufacturerInfo.setFloorPriceEffectManifest(manifest);
        OrderInfo order = new OrderInfo();
        order.setManufacturerInfo(manufacturerInfo);
        return order;
    }

    private ManufacturerInfo.FloorPriceEffectItem floor(String materialId, String price) {
        ManufacturerInfo.FloorPriceEffectItem item = new ManufacturerInfo.FloorPriceEffectItem();
        item.setRefId(materialId);
        item.setRefType("MATERIAL");
        item.setFloorPrice(price == null ? null : new BigDecimal(price));
        return item;
    }

    private OrderItem orderItem(String materialId, String actualPrice) {
        MaterialConfig material = new MaterialConfig();
        material.setMaterialId(materialId);
        OrderItemPriceInfo price = new OrderItemPriceInfo();
        price.setActualPrice(new BigDecimal(actualPrice));
        OrderItem item = new OrderItem();
        item.setMaterial(material);
        item.setPrice(price);
        return item;
    }
}

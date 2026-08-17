package com.mes.application.dto.req.order;

import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.vo.ManufacturerInfo;
import com.mes.domain.order.orderInfo.vo.OrderPriceInfo;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderAddRequestTest {

    @Test
    void toOrderItemsCopiesManufacturerSnapshotToEachItem() {
        OrderPriceInfo price = new OrderPriceInfo();
        price.setPaymentPrice(new BigDecimal("13"));

        ManufacturerInfo.FloorPriceEffectItem effectItem = new ManufacturerInfo.FloorPriceEffectItem();
        effectItem.setFloorPrice(new BigDecimal("1"));
        effectItem.setItemMergePrice(new BigDecimal("0.12"));
        effectItem.setRefId("material-id");
        effectItem.setRefType("MATERIAL");
        ManufacturerInfo.FloorPriceEffectManifest manifest = new ManufacturerInfo.FloorPriceEffectManifest();
        manifest.setFloorPriceEffectItems(List.of(effectItem));

        ManufacturerInfoRequest manufacturer = new ManufacturerInfoRequest();
        manufacturer.setId("manufacturer-id");
        manufacturer.setName("修水禾物工厂");
        manufacturer.setPrice(price);
        manufacturer.setFloorPriceEffectManifest(manifest);

        OrderItemRequest firstItem = new OrderItemRequest();
        firstItem.setId(1L);
        firstItem.setCount(1);
        OrderItemRequest secondItem = new OrderItemRequest();
        secondItem.setId(2L);
        secondItem.setCount(2);

        OrderAddRequest request = new OrderAddRequest();
        request.setManufacturerInfo(manufacturer);
        request.setOrderItems(List.of(firstItem, secondItem));
        Address address = new Address();
        address.setDetailAddress("测试地址");
        ConsigneeRequest consignee = new ConsigneeRequest();
        consignee.setAddress(address);
        request.setConsignee(consignee);
        Map<String, Object> rawOrder = Map.of("unknownOrderField", "retained");
        Map<String, Object> rawFirstItem = Map.of("unknownItemField", "first");
        Map<String, Object> rawSecondItem = Map.of("unknownItemField", "second");
        request.setSourceInput(rawOrder);
        request.setOrderItemSourceInputs(List.of(rawFirstItem, rawSecondItem));

        assertEquals(rawOrder, request.toOrderInfo().getSourceInput());

        List<OrderItem> items = request.toOrderItems();

        assertEquals(2, items.size());
        for (OrderItem item : items) {
            assertEquals("manufacturer-id", item.getManufacturerId());
            assertNotNull(item.getManufacturerInfo());
            assertEquals("manufacturer-id", item.getManufacturerInfo().getId());
            assertEquals("修水禾物工厂", item.getManufacturerInfo().getName());
            assertEquals(new BigDecimal("13"), item.getManufacturerInfo().getPrice().getPaymentPrice());
            assertEquals(effectItem,
                    item.getManufacturerInfo().getFloorPriceEffectManifest().getFloorPriceEffectItems().getFirst());
        }
        assertEquals(rawFirstItem, items.get(0).getSourceInput());
        assertEquals(rawSecondItem, items.get(1).getSourceInput());
    }
}

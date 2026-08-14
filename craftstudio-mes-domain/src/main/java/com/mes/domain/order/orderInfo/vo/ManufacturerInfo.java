package com.mes.domain.order.orderInfo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * The manufacturer snapshot supplied when an order is created.
 *
 * <p>The price and floor-price manifest are snapshots rather than references to
 * current manufacturer configuration, so they must be stored with the order.</p>
 */
@Data
public class ManufacturerInfo {
    private String id;
    private String name;
    private OrderPriceInfo price;
    private FloorPriceEffectManifest floorPriceEffectManifest;

    @Data
    public static class FloorPriceEffectManifest {
        private List<FloorPriceEffectItem> floorPriceEffectItems;
    }

    @Data
    public static class FloorPriceEffectItem {
        private BigDecimal floorPrice;
        private BigDecimal itemMergePrice;
        private String refId;
        private String refType;
    }
}

package com.mes.application.dto.req.order;

import com.mes.domain.order.orderInfo.vo.ManufacturerInfo;
import com.mes.domain.order.orderInfo.vo.OrderPriceInfo;
import lombok.Data;

@Data
public class ManufacturerInfoRequest {
    private String id;
    private String name;
    private OrderPriceInfo price;
    private ManufacturerInfo.FloorPriceEffectManifest floorPriceEffectManifest;

    public ManufacturerInfo toManufacturerInfo() {
        ManufacturerInfo result = new ManufacturerInfo();
        result.setId(id);
        result.setName(name);
        result.setPrice(price);
        result.setFloorPriceEffectManifest(floorPriceEffectManifest);
        return result;
    }
}

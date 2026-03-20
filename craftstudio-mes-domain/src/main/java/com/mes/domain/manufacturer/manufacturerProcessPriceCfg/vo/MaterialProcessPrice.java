package com.mes.domain.manufacturer.manufacturerProcessPriceCfg.vo;

import com.mes.domain.base.UnitPrice;
import lombok.Data;

@Data
public class MaterialProcessPrice {

    private String materialId;
    private String materialName;
    private UnitPrice processPrice;
    private Double basePrice;

}

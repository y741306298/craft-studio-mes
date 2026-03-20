package com.mes.domain.manufacturer.manufacturerMaterialPriceCfg.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.base.UnitPrice;
import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class ManufacturerMaterialPriceCfg extends BaseEntity {

    private String manufacturerId;              // 制造商 ID
    private String manufacturerName;            // 制造商名称
    private UnitPrice materialPrice;        // 材料价格配置

}

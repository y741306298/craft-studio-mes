package com.mes.domain.manufacturer.manufacturerMtsProductCfg.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.base.UnitPrice;
import com.mes.domain.manufacturer.enums.CfgStatus;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.vo.ManufacturerMtsProductSpec;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ManufacturerMtsProductCfg extends BaseEntity {
    private String manufacturerId;              // 制造商 ID
    private String productId;
    private String productName;
    private String productPreviewUrl;
    private List<ManufacturerMtsProductSpec> mtsProductSpecs;
    private CfgStatus status;
}

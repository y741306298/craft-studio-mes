package com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ManufacturerMaterialLayoutSpecCfg extends BaseEntity {
    /**
     * 制造商元数据 ID。
     */
    private String manufacturerMetaId;

    /**
     * 材料排版规格 ID。
     */
    private String materialLayoutSpecId;
}

package com.mes.infra.dal.manufacurer.manufacturerMaterialPriceCfg.po;

import com.mes.domain.manufacturer.manufacturerMaterialPriceCfg.entity.ManufacturerMaterialPriceCfg;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "manufacturerMaterialPriceCfg")
public class ManufacturerMaterialPricePo extends BasePO<ManufacturerMaterialPriceCfg> {


    @Override
    public ManufacturerMaterialPriceCfg toDO() {
        return null;
    }

    @Override
    protected BasePO<ManufacturerMaterialPriceCfg> fromDO(ManufacturerMaterialPriceCfg _do) {
        return null;
    }
}

package com.mes.infra.dal.manufacurer.manufacturerMaterialLayoutSpecCfg.po;

import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.entity.ManufacturerMaterialLayoutSpecCfg;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "manufacturer_material_layout_spec_cfg")
public class ManufacturerMaterialLayoutSpecCfgPo extends BasePO<ManufacturerMaterialLayoutSpecCfg> {

    private String manufacturerMetaId;
    private String materialLayoutSpecId;

    @Override
    public ManufacturerMaterialLayoutSpecCfg toDO() {
        ManufacturerMaterialLayoutSpecCfg cfg = new ManufacturerMaterialLayoutSpecCfg();
        copyBaseFieldsToDO(cfg);
        cfg.setManufacturerMetaId(manufacturerMetaId);
        cfg.setMaterialLayoutSpecId(materialLayoutSpecId);
        return cfg;
    }

    @Override
    protected BasePO<ManufacturerMaterialLayoutSpecCfg> fromDO(ManufacturerMaterialLayoutSpecCfg cfg) {
        this.manufacturerMetaId = cfg.getManufacturerMetaId();
        this.materialLayoutSpecId = cfg.getMaterialLayoutSpecId();
        return this;
    }
}

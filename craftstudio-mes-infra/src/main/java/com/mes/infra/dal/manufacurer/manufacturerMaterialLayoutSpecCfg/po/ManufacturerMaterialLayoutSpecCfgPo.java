package com.mes.infra.dal.manufacurer.manufacturerMaterialLayoutSpecCfg.po;

import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.entity.ManufacturerMaterialLayoutSpecCfg;
import com.mes.domain.manufacturer.materialLayoutSpec.entity.MaterialLayoutSpecStep;
import com.mes.domain.order.orderInfo.vo.MaterialConfig;
import com.mes.infra.base.BasePO;
import com.piliofpala.craftstudio.shared.domain.graphics.vo.Size3D;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "manufacturer_material_layout_spec_cfg")
public class ManufacturerMaterialLayoutSpecCfgPo extends BasePO<ManufacturerMaterialLayoutSpecCfg> {

    private String manufacturerMetaId;
    private String materialId;
    private MaterialConfig.MaterialSnapshot materialSnapshot;
    private Size3D usageSize3D;
    private List<MaterialLayoutSpecStep> insetSteps;

    @Override
    public ManufacturerMaterialLayoutSpecCfg toDO() {
        ManufacturerMaterialLayoutSpecCfg cfg = new ManufacturerMaterialLayoutSpecCfg();
        copyBaseFieldsToDO(cfg);
        cfg.setManufacturerMetaId(manufacturerMetaId);
        cfg.setMaterialId(materialId);
        cfg.setMaterialSnapshot(materialSnapshot);
        cfg.setUsageSize3D(usageSize3D);
        cfg.setInsetSteps(insetSteps);
        return cfg;
    }

    @Override
    protected BasePO<ManufacturerMaterialLayoutSpecCfg> fromDO(ManufacturerMaterialLayoutSpecCfg cfg) {
        this.manufacturerMetaId = cfg.getManufacturerMetaId();
        this.materialId = cfg.getMaterialId();
        this.materialSnapshot = cfg.getMaterialSnapshot();
        this.usageSize3D = cfg.getUsageSize3D();
        this.insetSteps = cfg.getInsetSteps();
        return this;
    }
}

package com.mes.application.dto.req.manufacturerMaterialLayoutSpecCfg;

import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.entity.ManufacturerMaterialLayoutSpecCfg;
import com.mes.domain.manufacturer.materialLayoutSpec.entity.MaterialLayoutSpecStep;
import com.mes.domain.order.orderInfo.vo.MaterialConfig;
import com.piliofpala.craftstudio.shared.domain.graphics.vo.Size3D;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ManufacturerMaterialLayoutSpecCfgRequest {
    private String id;

    @NotBlank(message = "manufacturerMetaId不能为空")
    private String manufacturerMetaId;

    @NotBlank(message = "materialId不能为空")
    private String materialId;

    private MaterialConfig.MaterialSnapshot materialSnapshot;

    private Size3D usageSize3D;

    @NotEmpty(message = "阶梯数据不能为空")
    private List<MaterialLayoutSpecStep> insetSteps;

    public ManufacturerMaterialLayoutSpecCfg toDomainEntity() {
        ManufacturerMaterialLayoutSpecCfg cfg = new ManufacturerMaterialLayoutSpecCfg();
        cfg.setId(id);
        cfg.setManufacturerMetaId(manufacturerMetaId);
        cfg.setMaterialId(materialId);
        cfg.setMaterialSnapshot(materialSnapshot);
        cfg.setUsageSize3D(usageSize3D);
        cfg.setInsetSteps(insetSteps);
        return cfg;
    }
}

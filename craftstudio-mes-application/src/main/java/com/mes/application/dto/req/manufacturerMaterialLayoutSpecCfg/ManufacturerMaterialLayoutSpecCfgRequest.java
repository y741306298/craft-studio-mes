package com.mes.application.dto.req.manufacturerMaterialLayoutSpecCfg;

import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.entity.ManufacturerMaterialLayoutSpecCfg;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ManufacturerMaterialLayoutSpecCfgRequest {
    private String id;

    @NotBlank(message = "manufacturerMetaId不能为空")
    private String manufacturerMetaId;

    @NotBlank(message = "materialLayoutSpecId不能为空")
    private String materialLayoutSpecId;

    public ManufacturerMaterialLayoutSpecCfg toDomainEntity() {
        ManufacturerMaterialLayoutSpecCfg cfg = new ManufacturerMaterialLayoutSpecCfg();
        cfg.setId(id);
        cfg.setManufacturerMetaId(manufacturerMetaId);
        cfg.setMaterialLayoutSpecId(materialLayoutSpecId);
        return cfg;
    }
}

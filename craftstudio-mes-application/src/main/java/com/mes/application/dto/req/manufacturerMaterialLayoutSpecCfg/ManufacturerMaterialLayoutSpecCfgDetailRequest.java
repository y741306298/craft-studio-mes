package com.mes.application.dto.req.manufacturerMaterialLayoutSpecCfg;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ManufacturerMaterialLayoutSpecCfgDetailRequest {
    @NotBlank(message = "manufacturerMetaId不能为空")
    private String manufacturerMetaId;

    @NotBlank(message = "materialId不能为空")
    private String materialId;
}

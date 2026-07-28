package com.mes.application.dto.req.productionpiece;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NormalizeProductionPieceStatusRequest {

    @NotBlank(message = "manufacturerMetaId 不能为空")
    private String manufacturerMetaId;
}

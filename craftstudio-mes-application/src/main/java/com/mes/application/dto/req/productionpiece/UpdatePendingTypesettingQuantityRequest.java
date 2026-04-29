package com.mes.application.dto.req.productionpiece;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePendingTypesettingQuantityRequest {

    @NotBlank(message = "productionPieceId 不能为空")
    private String productionPieceId;

    @Min(value = 1, message = "增加数量必须大于 0")
    private Integer increaseQuantity;
}

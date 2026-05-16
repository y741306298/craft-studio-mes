package com.mes.application.dto.req.productionpiece;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchRedoProductionPieceRequest {

    @NotEmpty(message = "productionPieceIds 不能为空")
    private List<String> productionPieceIds;

    @Min(value = 1, message = "增加数量必须大于 0")
    private Integer increaseQuantity;
}

package com.mes.application.dto.req.productionpiece;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class BatchRedoProductionPieceRequest {

    @NotEmpty(message = "pieces 不能为空")
    @Valid
    private List<PieceRedoItem> pieces;

    @Data
    public static class PieceRedoItem {
        @NotBlank(message = "productionPieceId 不能为空")
        private String productionPieceId;

        @NotNull(message = "increaseQuantity 不能为空")
        @Min(value = 1, message = "增加数量必须大于 0")
        private Integer increaseQuantity;
    }
}

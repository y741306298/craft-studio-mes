package com.mes.application.dto.req.typesetting;

import com.mes.application.dto.req.base.ApiRequest;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ConfirmPrintRequest extends ApiRequest {

    @NotEmpty(message = "生产工件 ID 列表不能为空")
    private List<String> productionPieceIds;

    @Override
    public boolean isValid() {
        return productionPieceIds != null && !productionPieceIds.isEmpty();
    }

    @Override
    public String getValidationMessage() {
        if (productionPieceIds == null || productionPieceIds.isEmpty()) {
            return "生产工件 ID 列表不能为空";
        }
        return null;
    }
}

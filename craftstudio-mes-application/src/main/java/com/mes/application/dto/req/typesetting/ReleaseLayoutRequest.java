package com.mes.application.dto.req.typesetting;

import com.mes.application.dto.req.base.ApiRequest;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ReleaseLayoutRequest extends ApiRequest {

    @NotEmpty(message = "排版 ID 列表不能为空")
    private List<String> typesettingIds;

    @Override
    public boolean isValid() {
        return typesettingIds != null && !typesettingIds.isEmpty();
    }

    @Override
    public String getValidationMessage() {
        if (typesettingIds == null || typesettingIds.isEmpty()) {
            return "排版 ID 列表不能为空";
        }
        return null;
    }
}

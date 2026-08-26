package com.mes.application.dto.req.statistics;

import com.mes.application.dto.req.base.ApiRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Query parameters for factories that received orders from a source factory. */
@Data
@EqualsAndHashCode(callSuper = true)
public class TransferTargetFactoryRequest extends ApiRequest {
    @NotBlank(message = "来源工厂不能为空")
    private String sourceId;
    @NotBlank(message = "开始日期不能为空")
    private String createDateStart;
    @NotBlank(message = "结束日期不能为空")
    private String createDateEnd;

    @Override
    public boolean isValid() {
        return sourceId != null && !sourceId.isBlank()
                && createDateStart != null && !createDateStart.isBlank()
                && createDateEnd != null && !createDateEnd.isBlank();
    }

    @Override
    public String getValidationMessage() {
        return "来源工厂、开始日期和结束日期不能为空";
    }
}

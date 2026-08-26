package com.mes.application.dto.req.statistics;

import com.mes.application.dto.req.base.ApiRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Query parameters for factories that transferred orders to a target factory. */
@Data
@EqualsAndHashCode(callSuper = true)
public class TransferSourceFactoryRequest extends ApiRequest {
    @NotBlank(message = "目标工厂不能为空")
    private String targetId;
    @NotBlank(message = "开始日期不能为空")
    private String createDateStart;
    @NotBlank(message = "结束日期不能为空")
    private String createDateEnd;

    @Override
    public boolean isValid() {
        return targetId != null && !targetId.isBlank()
                && createDateStart != null && !createDateStart.isBlank()
                && createDateEnd != null && !createDateEnd.isBlank();
    }

    @Override
    public String getValidationMessage() {
        return "目标工厂、开始日期和结束日期不能为空";
    }
}

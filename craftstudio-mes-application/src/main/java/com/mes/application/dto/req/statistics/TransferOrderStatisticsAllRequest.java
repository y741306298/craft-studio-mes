package com.mes.application.dto.req.statistics;

import com.mes.application.dto.req.base.ApiRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Request for an unpaged transfer statistics and transferred order-item details query. */
@Data
@EqualsAndHashCode(callSuper = true)
public class TransferOrderStatisticsAllRequest extends ApiRequest {
    private String sourceId;
    private String targetId;
    @NotBlank(message = "开始日期不能为空")
    private String createDateStart;
    @NotBlank(message = "结束日期不能为空")
    private String createDateEnd;

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String getValidationMessage() {
        return "";
    }
}

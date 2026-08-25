package com.mes.application.dto.req.statistics;

import com.mes.application.dto.req.base.PagedApiRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Request for transfer statistics and transferred order-item details. */
@Data
@EqualsAndHashCode(callSuper = true)
public class TransferOrderStatisticsRequest extends PagedApiRequest {
    @NotBlank(message = "源工厂 ID 不能为空")
    private String sourceId;
    @NotBlank(message = "目标工厂 ID 不能为空")
    private String targetId;
    @NotBlank(message = "开始日期不能为空")
    private String createDateStart;
    @NotBlank(message = "结束日期不能为空")
    private String createDateEnd;
}

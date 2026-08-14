package com.mes.application.dto.req.statistics;

import com.mes.application.dto.req.base.ApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Non-paged query for all statistics dimensions in a date range. */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderStatisticsFiltersRequest extends ApiRequest {
    private String manufacturerId;
    private String createDateStart;
    private String createDateEnd;

    @Override
    public boolean isValid() {
        return manufacturerId != null && !manufacturerId.isBlank()
                && createDateStart != null && !createDateStart.isBlank()
                && createDateEnd != null && !createDateEnd.isBlank();
    }

    @Override
    public String getValidationMessage() {
        return "工厂、开始日期和结束日期不能为空";
    }
}

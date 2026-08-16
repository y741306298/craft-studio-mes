package com.mes.application.dto.req.statistics;

import com.mes.application.dto.req.base.ApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Request for an unpaged order statistics query. */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderStatisticsAllRequest extends ApiRequest {
    private String manufacturerId;
    private String orderId;
    private String routeId;
    private String createDateStart;
    private String createDateEnd;
    private String materialId;
    private String materialName;
    private String materialType;
    private String orgName;

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String getValidationMessage() {
        return "";
    }
}

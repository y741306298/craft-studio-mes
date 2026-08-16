package com.mes.application.dto.req.statistics;

import com.mes.application.dto.req.base.PagedApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderStatisticsListRequest extends PagedApiRequest {
    private String manufacturerId;
    private String orderId;
    private String routeId;
    private String createDateStart;
    private String createDateEnd;
    private String materialId;
    private String materialName;
    private String materialType;
    private String orgName;
}

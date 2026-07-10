package com.mes.application.dto.req.order;

import com.mes.application.dto.req.base.PagedApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderListRequest extends PagedApiRequest {
    private String manufacturerId;
    private String orderId;
    private String status;
    private String customerName;
    private String customerPhone;
    private String createDateStart;
    private String createDateEnd;
    /**
     * 订单统计查询日期，格式 yyyy-MM-dd。
     */
    private String statisticsDate;
    /**
     * 订单统计所属工厂 ID；为空时兼容使用 manufacturerId。
     */
    private String manufacturerMetaId;
    private String routeId;
    private String orgName;
}

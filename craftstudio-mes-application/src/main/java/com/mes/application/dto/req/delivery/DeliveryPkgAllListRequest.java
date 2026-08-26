package com.mes.application.dto.req.delivery;

import lombok.Data;

/**
 * 包裹全量查询条件，不包含分页字段。
 */
@Data
public class DeliveryPkgAllListRequest {
    private String manufacturerMetaId;
    private String orderId;
    private String recipientName;
    private String recipientPhone;
    private String kuaidiNum;
    private String createTimeStart;
    private String createTimeEnd;
    private String status;
}

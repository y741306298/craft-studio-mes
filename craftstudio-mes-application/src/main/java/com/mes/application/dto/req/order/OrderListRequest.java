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
    private String customerPhone;
    private String createDateStart;
    private String createDateEnd;
}

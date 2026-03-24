package com.mes.interfaces.api.dto.req.order;

import com.mes.interfaces.api.dto.req.base.PagedApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderListRequest extends PagedApiRequest {
    private String orderId;
    private String status;
}

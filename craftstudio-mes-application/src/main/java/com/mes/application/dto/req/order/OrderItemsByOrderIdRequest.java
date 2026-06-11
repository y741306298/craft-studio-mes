package com.mes.application.dto.req.order;

import lombok.Data;

@Data
public class OrderItemsByOrderIdRequest {
    private String manufacturerId;
    private String orderId;
}

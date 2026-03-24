package com.mes.interfaces.api.dto.resp.order;

import com.mes.domain.order.orderInfo.entity.OrderInfo;
import lombok.Data;

/**
 * 订单信息响应 DTO
 */
@Data
public class OrderInfoResponse {
    private String id;
    private String orderId;
    private Object customer;
    private String deliveryAddress;
    private String status;

    public static OrderInfoResponse from(OrderInfo orderInfo) {
        OrderInfoResponse response = new OrderInfoResponse();
        response.setId(orderInfo.getId());
        response.setOrderId(orderInfo.getOrderId());
        response.setCustomer(orderInfo.getCustomer());
        response.setDeliveryAddress(orderInfo.getDeliveryAddress());
        response.setStatus(orderInfo.getStatus() != null ? orderInfo.getStatus().getCode() : null);
        return response;
    }
}
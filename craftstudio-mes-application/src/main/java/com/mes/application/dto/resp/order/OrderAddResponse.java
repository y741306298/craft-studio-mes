package com.mes.application.dto.resp.order;

import com.mes.domain.order.orderInfo.entity.OrderInfo;
import lombok.Data;

/**
 * 新增订单响应 DTO。
 */
@Data
public class OrderAddResponse {
    /** 订单 ID */
    private String orderId;

    /** 快递100预下单运单号；未预打印时为空 */
    private String kuaidiNum;

    public static OrderAddResponse from(OrderInfo orderInfo) {
        OrderAddResponse response = new OrderAddResponse();
        if (orderInfo != null) {
            response.setOrderId(orderInfo.getOrderId());
            response.setKuaidiNum(orderInfo.getKuaidiNum());
        }
        return response;
    }
}

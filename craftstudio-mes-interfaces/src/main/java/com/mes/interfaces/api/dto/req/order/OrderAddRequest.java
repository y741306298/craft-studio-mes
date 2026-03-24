package com.mes.interfaces.api.dto.req.order;

import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import lombok.Data;

import java.util.List;

/**
 * 订单新增请求 DTO
 */
@Data
public class OrderAddRequest {
    private OrderInfo orderInfo;
    private List<OrderItem> orderItems;
}
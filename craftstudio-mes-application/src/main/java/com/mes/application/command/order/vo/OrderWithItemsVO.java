package com.mes.application.command.order.vo;

import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import lombok.Data;

import java.util.List;

@Data
public class OrderWithItemsVO {
    private OrderInfo orderInfo;
    private List<OrderItem> orderItems;
}

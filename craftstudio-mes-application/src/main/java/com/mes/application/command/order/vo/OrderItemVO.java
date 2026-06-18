package com.mes.application.command.order.vo;

import com.mes.domain.delivery.deliveryRoute.vo.OrgInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.vo.OrderCustomer;
import lombok.Data;

@Data
public class OrderItemVO extends OrderItem {

    private OrderCustomer customer;

    private String remark;

    private OrgInfo orgInfo;

}

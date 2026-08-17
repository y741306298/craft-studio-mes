package com.mes.application.command.order.vo;

import com.mes.domain.delivery.deliveryRoute.vo.OrgInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.vo.ManufacturerInfo;
import com.mes.domain.order.orderInfo.vo.OrderCustomer;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemVO extends OrderItem {

    private OrderCustomer customer;

    private String remark;

    private OrgInfo orgInfo;

    /**
     * 所属订单的厂家信息快照。
     */
    private ManufacturerInfo manufacturerInfo;

    /**
     * 所属订单 orderInfo.price.paymentPrice。
     */
    private BigDecimal paymentPrice;

}

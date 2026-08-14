package com.mes.application.command.order.vo;

import com.mes.domain.delivery.deliveryRoute.vo.OrgInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.vo.OrderCustomer;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemVO extends OrderItem {

    private OrderCustomer customer;

    private String remark;

    private OrgInfo orgInfo;

    /**
     * 所属订单 orderInfo.price.paymentPrice。
     */
    private BigDecimal paymentPrice;

    /** 按底价清单/支付价回退规则计算的实际统计金额。 */
    private BigDecimal orderItemPrice;

    /** 订单项 routeId 对应的配送路线名称。 */
    private String routeName;

}

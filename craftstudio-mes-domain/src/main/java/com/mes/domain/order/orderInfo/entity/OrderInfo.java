package com.mes.domain.order.orderInfo.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.delivery.deliveryNet.entity.DeliveryWay;
import com.mes.domain.delivery.deliveryNet.enums.DeliveryWayNUM;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.vo.LogisticsCarrierInfo;
import com.mes.domain.order.orderInfo.vo.OrderCustomer;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderInfo extends BaseEntity {

    private String orderId;
    private OrderCustomer customer;
    private String deliveryAddress;
    private OrderStatus status;
    private Date expectedDeliveryDate;

}

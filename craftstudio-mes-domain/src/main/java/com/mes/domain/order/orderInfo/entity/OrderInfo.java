package com.mes.domain.order.orderInfo.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.delivery.deliveryNet.entity.DeliveryWay;
import com.mes.domain.delivery.deliveryNet.enums.DeliveryWayNUM;
import com.mes.domain.delivery.deliveryRoute.vo.OrgInfo;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.vo.LogisticsCarrierInfo;
import com.mes.domain.order.orderInfo.vo.OrderChannelInfo;
import com.mes.domain.order.orderInfo.vo.OrderCustomer;
import com.mes.domain.order.orderInfo.vo.OrderPriceInfo;
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
    private String remark;
    private String platformCode;
    private String routeId;
    private String routeNodeId;
    private OrgInfo orgInfo;
    private OrderPriceInfo price;
    private Long orgId;
    private Long userId;
    private String externalOrderId;
    private String paymentState;
    private OrderChannelInfo channel;
    private LogisticsCarrierInfo logisticsCarrierInfo;
    private String manufacturerId;
    private String manufacturerName;

    /** 预下单的快递100运单号，用于首次 addPkg 复打面单 */
    private String kuaidiNum;

}

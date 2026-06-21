package com.mes.infra.dal.order.po;

import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.delivery.deliveryRoute.vo.OrgInfo;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.vo.LogisticsCarrierInfo;
import com.mes.domain.order.orderInfo.vo.OrderCustomer;
import com.mes.domain.order.orderInfo.vo.OrderPriceInfo;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "orderInfo")
public class OrderInfoPo extends BasePO<OrderInfo> {

    private String orderId;
    private OrderCustomer customer;
    private String deliveryAddress;
    private String status;
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
    private LogisticsCarrierInfo logisticsCarrierInfo;
    private String manufacturerId;
    private String manufacturerName;

    @Override
    public OrderInfo toDO() {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(getId());
        orderInfo.setCreateTime(getCreateTime());
        orderInfo.setUpdateTime(getUpdateTime());

        orderInfo.setOrderId(this.orderId);
        orderInfo.setCustomer(this.customer);
        orderInfo.setDeliveryAddress(this.deliveryAddress);
        orderInfo.setRemark(this.remark);
        orderInfo.setPlatformCode(this.platformCode);
        orderInfo.setRouteId(this.routeId);
        orderInfo.setRouteNodeId(this.routeNodeId);
        orderInfo.setOrgInfo(this.orgInfo);
        orderInfo.setPrice(this.price);
        orderInfo.setOrgId(this.orgId);
        orderInfo.setUserId(this.userId);
        orderInfo.setExternalOrderId(this.externalOrderId);
        orderInfo.setPaymentState(this.paymentState);
        orderInfo.setLogisticsCarrierInfo(this.logisticsCarrierInfo);
        orderInfo.setManufacturerId(this.manufacturerId);
        orderInfo.setManufacturerName(this.manufacturerName);
        if (this.status != null) {
            orderInfo.setStatus(OrderStatus.getByCode(this.status));
        }

        return orderInfo;
    }

    @Override
    protected BasePO<OrderInfo> fromDO(OrderInfo _do) {
        this.orderId = _do.getOrderId();
        this.customer = _do.getCustomer();
        this.deliveryAddress = _do.getDeliveryAddress();
        this.remark = _do.getRemark();
        this.platformCode = _do.getPlatformCode();
        this.routeId = _do.getRouteId();
        this.routeNodeId = _do.getRouteNodeId();
        this.orgInfo = _do.getOrgInfo();
        this.price = _do.getPrice();
        this.orgId = _do.getOrgId();
        this.userId = _do.getUserId();
        this.externalOrderId = _do.getExternalOrderId();
        this.paymentState = _do.getPaymentState();
        this.logisticsCarrierInfo = _do.getLogisticsCarrierInfo();
        this.manufacturerId = _do.getManufacturerId();
        this.manufacturerName = _do.getManufacturerName();
        this.status = _do.getStatus() != null ? _do.getStatus().getCode() : null;
        return this;
    }
}

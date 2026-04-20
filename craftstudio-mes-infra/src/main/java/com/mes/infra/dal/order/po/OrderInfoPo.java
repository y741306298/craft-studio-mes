package com.mes.infra.dal.order.po;

import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.vo.LogisticsCarrierInfo;
import com.mes.domain.order.orderInfo.vo.OrderCustomer;
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

    @Override
    public OrderInfo toDO() {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(getId());
        orderInfo.setCreateTime(getCreateTime());
        orderInfo.setUpdateTime(getUpdateTime());

        orderInfo.setOrderId(this.orderId);
        orderInfo.setCustomer(this.customer);
        orderInfo.setDeliveryAddress(this.deliveryAddress);
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
        this.status = _do.getStatus() != null ? _do.getStatus().getCode() : null;
        return this;
    }
}

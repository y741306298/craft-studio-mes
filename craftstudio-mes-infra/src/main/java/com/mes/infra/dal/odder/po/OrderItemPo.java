package com.mes.infra.dal.order.po;

import com.mes.domain.manufacturer.manufacturerMtsProductCfg.vo.ManufacturerMtsProductSpec;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "orderItem")
public class OrderItemPo extends BasePO<OrderItem> {

    private String orderItemId;
    private String orderId;
    private ManufacturerMtsProductSpec mtsProduct;
    private String procedureFlowId;
    private Integer quantity;
    private String status;
    private Boolean isUrgent;

    @Override
    public OrderItem toDO() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(getId());
        orderItem.setCreateTime(getCreateTime());
        orderItem.setUpdateTime(getUpdateTime());

        orderItem.setOrderItemId(this.orderItemId);
        orderItem.setOrderId(this.orderId);
        orderItem.setMtsProduct(this.mtsProduct);
        orderItem.setProcedureFlowId(this.procedureFlowId);
        orderItem.setQuantity(this.quantity);
        orderItem.setStatus(this.status);
        orderItem.setIsUrgent(this.isUrgent);

        return orderItem;
    }

    @Override
    protected BasePO<OrderItem> fromDO(OrderItem _do) {
        this.orderItemId = _do.getOrderItemId();
        this.orderId = _do.getOrderId();
        this.mtsProduct = _do.getMtsProduct();
        this.procedureFlowId = _do.getProcedureFlowId();
        this.quantity = _do.getQuantity();
        this.status = _do.getStatus();
        this.isUrgent = _do.getIsUrgent();
        return this;
    }
}

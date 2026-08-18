package com.mes.infra.dal.order.orderItemPriceAllocation.po;

import com.mes.domain.order.orderItemPriceAllocation.entity.OrderItemPriceAllocation;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "orderItemPriceAllocation")
public class OrderItemPriceAllocationPo extends BasePO<OrderItemPriceAllocation> {
    private String orderItemId;
    private String manufacturerMetaId;
    private BigDecimal price;

    @Override
    public OrderItemPriceAllocation toDO() {
        OrderItemPriceAllocation allocation = new OrderItemPriceAllocation();
        allocation.setId(getId());
        allocation.setCreateTime(getCreateTime());
        allocation.setUpdateTime(getUpdateTime());
        allocation.setOrderItemId(orderItemId);
        allocation.setManufacturerMetaId(manufacturerMetaId);
        allocation.setPrice(price);
        return allocation;
    }

    @Override
    protected BasePO<OrderItemPriceAllocation> fromDO(OrderItemPriceAllocation allocation) {
        orderItemId = allocation.getOrderItemId();
        manufacturerMetaId = allocation.getManufacturerMetaId();
        price = allocation.getPrice();
        return this;
    }
}

package com.mes.domain.order.orderItemPriceAllocation.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderItemPriceAllocation extends BaseEntity {
    private String orderItemId;
    private String manufacturerMetaId;
    private BigDecimal price;
}

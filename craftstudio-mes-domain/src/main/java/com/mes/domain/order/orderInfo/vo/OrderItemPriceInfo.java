package com.mes.domain.order.orderInfo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemPriceInfo {
    private BigDecimal originalPrice;
    private BigDecimal actualPrice;
}

package com.mes.domain.order.orderInfo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderPriceInfo {
    private BigDecimal originalPrice;
    private BigDecimal actualPrice;
    private BigDecimal oriActualPrice;
    private BigDecimal logisticsPrice;
    private BigDecimal paymentPrice;
}

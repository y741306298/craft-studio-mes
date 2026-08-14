package com.mes.application.command.statistics.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsItemVO {
    private String orderId;
    private BigDecimal paymentPrice;
    /** The snapshot amount that actually participates in daily statistics. */
    private BigDecimal orderItemPrice;
    private Date createTime;
}

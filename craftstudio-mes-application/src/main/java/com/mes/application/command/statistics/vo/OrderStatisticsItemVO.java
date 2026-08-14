package com.mes.application.command.statistics.vo;

import com.mes.domain.order.orderInfo.entity.OrderItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderStatisticsItemVO extends OrderItem {
    private String routeName;
    private BigDecimal paymentPrice;
    /** The snapshot amount that actually participates in daily statistics. */
    private BigDecimal orderItemPrice;
}

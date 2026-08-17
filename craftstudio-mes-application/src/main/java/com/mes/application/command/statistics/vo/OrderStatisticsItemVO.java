package com.mes.application.command.statistics.vo;

import com.mes.domain.order.orderInfo.entity.OrderItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderStatisticsItemVO extends OrderItem {
    private String routeName;
    private java.math.BigDecimal paymentPrice;
}

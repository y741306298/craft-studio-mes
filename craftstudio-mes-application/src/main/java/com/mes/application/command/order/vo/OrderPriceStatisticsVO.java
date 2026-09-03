package com.mes.application.command.order.vo;

import com.mes.domain.order.orderInfo.entity.OrderInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class OrderPriceStatisticsVO {
    private List<OrderInfo> orderInfos;
    private BigDecimal actualPriceTotal;
    private BigDecimal logisticsPriceTotal;
    private BigDecimal paymentPriceTotal;
}

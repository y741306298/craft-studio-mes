package com.mes.application.dto.resp.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsResponse {
    /** 订单总数（按 orderId 去重）。 */
    private Long totalOrderCount;

    /** 订单项总面积（平方米）。 */
    private BigDecimal totalArea;

    /** 订单总金额（按 orderId 去重汇总）。 */
    private BigDecimal totalAmount;
}

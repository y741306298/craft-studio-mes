package com.mes.application.command.statistics.vo;

import com.mes.application.command.order.vo.OrderItemVO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** A page of transferred order items plus persisted transfer totals. */
@Data
@AllArgsConstructor
public class TransferOrderStatisticsVO {
    private List<OrderItemVO> items;
    private long total;
    private Long totalOrderCount;
    private BigDecimal totalAmount;
}

package com.mes.domain.order.orderStatistics.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderDailyStatistics extends BaseEntity {
    private String manufacturerMetaId;
    private LocalDate statisticsDate;
    private String indexId;
    private String indexName;
    private OrderStatisticsType type;
    private Long totalOrderCount;
    private BigDecimal totalArea;
    private BigDecimal totalAmount;
}

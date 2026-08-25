package com.mes.domain.order.transferStatistics.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Daily transfer totals for one source-factory to target-factory route. */
@Data
@EqualsAndHashCode(callSuper = true)
public class TransferDailyStatistics extends BaseEntity {
    private String sourceId;
    private String targetId;
    private String targetName;
    private LocalDate statisticsDate;
    private Long totalOrderCount;
    private BigDecimal totalAmount;
}

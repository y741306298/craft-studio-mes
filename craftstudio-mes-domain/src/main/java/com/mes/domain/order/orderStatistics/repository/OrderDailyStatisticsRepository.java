package com.mes.domain.order.orderStatistics.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.order.orderStatistics.entity.OrderDailyStatistics;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface OrderDailyStatisticsRepository extends BaseRepository<OrderDailyStatistics> {
    OrderDailyStatistics findByManufacturerMetaIdAndStatisticsDate(String manufacturerMetaId, LocalDate statisticsDate);

    OrderDailyStatistics increment(String manufacturerMetaId,
                                   LocalDate statisticsDate,
                                   long orderCount,
                                   BigDecimal area,
                                   BigDecimal amount);
}

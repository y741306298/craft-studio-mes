package com.mes.domain.order.orderStatistics.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.order.orderStatistics.entity.OrderDailyStatistics;
import com.mes.domain.order.orderStatistics.entity.OrderStatisticsType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface OrderDailyStatisticsRepository extends BaseRepository<OrderDailyStatistics> {
    OrderDailyStatistics find(String manufacturerMetaId, LocalDate statisticsDate,
                              String indexId, OrderStatisticsType type);

    OrderDailyStatistics sum(String manufacturerMetaId, LocalDate startDate, LocalDate endDate,
                             String indexId, OrderStatisticsType type);

    OrderDailyStatistics sumByIndexName(String manufacturerMetaId, LocalDate startDate, LocalDate endDate,
                                        String indexName, OrderStatisticsType type);

    OrderDailyStatistics increment(String manufacturerMetaId,
                                   LocalDate statisticsDate,
                                   String indexId,
                                   String indexName,
                                   OrderStatisticsType type,
                                   long orderCount,
                                   BigDecimal area,
                                   BigDecimal amount);

    List<OrderDailyStatistics> list(String manufacturerMetaId, LocalDate startDate, LocalDate endDate);
}

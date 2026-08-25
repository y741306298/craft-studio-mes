package com.mes.domain.order.transferStatistics.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.order.transferStatistics.entity.TransferDailyStatistics;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TransferDailyStatisticsRepository extends BaseRepository<TransferDailyStatistics> {
    TransferDailyStatistics increment(String sourceId, String targetId, String targetName,
                                      LocalDate statisticsDate, long orderCount, BigDecimal amount);

    TransferDailyStatistics sum(String sourceId, String targetId, LocalDate startDate, LocalDate endDate);
}

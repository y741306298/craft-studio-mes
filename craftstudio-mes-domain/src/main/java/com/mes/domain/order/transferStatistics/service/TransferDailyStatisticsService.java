package com.mes.domain.order.transferStatistics.service;

import com.mes.domain.order.transferStatistics.entity.TransferDailyStatistics;
import com.mes.domain.order.transferStatistics.repository.TransferDailyStatisticsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class TransferDailyStatisticsService {
    @Autowired
    private TransferDailyStatisticsRepository repository;

    public TransferDailyStatistics increment(String sourceId, String targetId, String targetName,
                                             LocalDate statisticsDate, long orderCount, BigDecimal amount) {
        return repository.increment(sourceId, targetId, targetName, statisticsDate, orderCount,
                amount == null ? BigDecimal.ZERO : amount);
    }

    public TransferDailyStatistics sum(String sourceId, String targetId,
                                       LocalDate startDate, LocalDate endDate) {
        return repository.sum(sourceId, targetId, startDate, endDate);
    }

    public void deleteRange(String sourceId, LocalDate startDate, LocalDate endDate) {
        repository.deleteRange(sourceId, startDate, endDate);
    }
}

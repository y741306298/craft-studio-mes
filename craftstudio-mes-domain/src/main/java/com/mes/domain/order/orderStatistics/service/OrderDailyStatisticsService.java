package com.mes.domain.order.orderStatistics.service;

import com.mes.domain.order.orderStatistics.entity.OrderDailyStatistics;
import com.mes.domain.order.orderStatistics.entity.OrderStatisticsType;
import com.mes.domain.order.orderStatistics.repository.OrderDailyStatisticsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class OrderDailyStatisticsService {
    @Autowired
    private OrderDailyStatisticsRepository repository;

    public OrderDailyStatistics increment(String manufacturerMetaId,
                                          LocalDate statisticsDate,
                                          String indexId,
                                          OrderStatisticsType type,
                                          long orderCount,
                                          BigDecimal area,
                                          BigDecimal amount) {
        return repository.increment(
                manufacturerMetaId,
                statisticsDate,
                indexId,
                type,
                orderCount,
                area == null ? BigDecimal.ZERO : area,
                amount == null ? BigDecimal.ZERO : amount
        );
    }

    public OrderDailyStatistics findByManufacturerMetaIdAndStatisticsDate(String manufacturerMetaId, LocalDate statisticsDate) {
        return find(manufacturerMetaId, statisticsDate, manufacturerMetaId, OrderStatisticsType.ENTERPRISE);
    }

    public OrderDailyStatistics find(String manufacturerMetaId, LocalDate statisticsDate,
                                     String indexId, OrderStatisticsType type) {
        return repository.find(manufacturerMetaId, statisticsDate, indexId, type);
    }

    public OrderDailyStatistics sumByManufacturerMetaIdAndStatisticsDateBetween(String manufacturerMetaId,
                                                                                LocalDate startDate,
                                                                                LocalDate endDate) {
        return sum(manufacturerMetaId, startDate, endDate,
                manufacturerMetaId, OrderStatisticsType.ENTERPRISE);
    }

    public OrderDailyStatistics sum(String manufacturerMetaId, LocalDate startDate, LocalDate endDate,
                                    String indexId, OrderStatisticsType type) {
        return repository.sum(manufacturerMetaId, startDate, endDate, indexId, type);
    }
}

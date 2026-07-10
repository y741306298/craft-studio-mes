package com.mes.domain.order.orderStatistics.service;

import com.mes.domain.order.orderStatistics.entity.OrderDailyStatistics;
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
                                          long orderCount,
                                          BigDecimal area,
                                          BigDecimal amount) {
        return repository.increment(
                manufacturerMetaId,
                statisticsDate,
                orderCount,
                area == null ? BigDecimal.ZERO : area,
                amount == null ? BigDecimal.ZERO : amount
        );
    }

    public OrderDailyStatistics findByManufacturerMetaIdAndStatisticsDate(String manufacturerMetaId, LocalDate statisticsDate) {
        return repository.findByManufacturerMetaIdAndStatisticsDate(manufacturerMetaId, statisticsDate);
    }
}

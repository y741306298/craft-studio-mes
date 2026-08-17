package com.mes.application.command.order;

import com.mes.domain.order.orderStatistics.entity.OrderDailyStatistics;
import com.mes.domain.order.orderStatistics.entity.OrderStatisticsType;
import com.mes.domain.order.orderStatistics.service.OrderDailyStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppOrderServiceStatisticsTest {
    private final OrderDailyStatisticsService statisticsService = mock(OrderDailyStatisticsService.class);
    private final AppOrderService appOrderService = new AppOrderService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(appOrderService, "orderDailyStatisticsService", statisticsService);
    }

    @Test
    void dailyTotalsUseEnterpriseDimension() {
        LocalDate statisticsDate = LocalDate.of(2026, 8, 17);
        OrderDailyStatistics expected = new OrderDailyStatistics();
        when(statisticsService.findByManufacturerMetaIdAndStatisticsDate("manufacturer-1", statisticsDate))
                .thenReturn(expected);

        OrderDailyStatistics actual = appOrderService.findOrderDailyStatistics("manufacturer-1", statisticsDate);

        assertSame(expected, actual);
        verify(statisticsService).findByManufacturerMetaIdAndStatisticsDate("manufacturer-1", statisticsDate);
    }

    @Test
    void unfilteredRangeTotalsUseEnterpriseDimension() {
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 17);
        Date startTime = toDate(startDate);
        Date endTime = toDate(endDate);
        OrderDailyStatistics expected = new OrderDailyStatistics();
        when(statisticsService.sum("manufacturer-1", startDate, endDate,
                "manufacturer-1", OrderStatisticsType.ENTERPRISE)).thenReturn(expected);

        OrderDailyStatistics actual = ReflectionTestUtils.invokeMethod(appOrderService,
                "findPersistedStatisticsTotals", "manufacturer-1", startTime, endTime,
                null, null, null);

        assertSame(expected, actual);
        verify(statisticsService).sum("manufacturer-1", startDate, endDate,
                "manufacturer-1", OrderStatisticsType.ENTERPRISE);
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant());
    }
}

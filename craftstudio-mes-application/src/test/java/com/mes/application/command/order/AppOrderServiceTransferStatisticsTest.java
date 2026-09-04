package com.mes.application.command.order;

import com.mes.application.command.statistics.vo.TransferOrderStatisticsVO;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.order.orderTransferRecord.entity.OrderTransferRecord;
import com.mes.domain.order.orderTransferRecord.service.OrderTransferRecordService;
import com.mes.domain.order.transferStatistics.entity.TransferDailyStatistics;
import com.mes.domain.order.transferStatistics.service.TransferDailyStatisticsService;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppOrderServiceTransferStatisticsTest {

    @Test
    void shouldCalculateTransferAmountUsingOrderStatisticsBeforeAfterDelta() {
        AppOrderService service = spy(new AppOrderService());
        OrderInfo orderInfo = new OrderInfo();
        List<OrderItem> itemsBeforeTransfer = List.of(new OrderItem());
        List<OrderItem> itemsAfterTransfer = List.of(new OrderItem());
        doReturn(new BigDecimal("138.575")).when(service)
                .calculateStatisticsAmount(orderInfo, itemsBeforeTransfer);
        doReturn(new BigDecimal("78.325")).when(service)
                .calculateStatisticsAmount(orderInfo, itemsAfterTransfer);

        BigDecimal result = service.calculateTransferStatisticsAmount(
                orderInfo, itemsBeforeTransfer, itemsAfterTransfer);

        assertThat(result).isEqualByComparingTo("60.25");
    }

    @Test
    void shouldSupportTargetOnlyTransferInQuery() {
        AppOrderService service = new AppOrderService();
        OrderTransferRecordService recordService = mock(OrderTransferRecordService.class);
        TransferDailyStatisticsService statisticsService = mock(TransferDailyStatisticsService.class);
        ReflectionTestUtils.setField(service, "orderTransferRecordService", recordService);
        ReflectionTestUtils.setField(service, "transferDailyStatisticsService", statisticsService);
        Date start = date(2026, 8, 1);
        Date end = date(2026, 8, 31);
        when(recordService.findAllTransferRecords(null, "T1", start, end)).thenReturn(List.of());

        TransferOrderStatisticsVO result = service.findTransferOrderStatistics(
                null, "T1", start, end, new PagedQuery(1, 20));

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalOrderCount()).isZero();
        verify(statisticsService).sum(null, "T1", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
    }

    @Test
    void shouldReturnTransferredItemsAndReadPersistedTotals() {
        AppOrderService service = new AppOrderService();
        OrderTransferRecordService recordService = mock(OrderTransferRecordService.class);
        OrderItemService itemService = mock(OrderItemService.class);
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        TransferDailyStatisticsService statisticsService = mock(TransferDailyStatisticsService.class);
        ReflectionTestUtils.setField(service, "orderTransferRecordService", recordService);
        ReflectionTestUtils.setField(service, "domainOrderItemService", itemService);
        ReflectionTestUtils.setField(service, "domainOrderInfoService", orderInfoService);
        ReflectionTestUtils.setField(service, "transferDailyStatisticsService", statisticsService);

        Date start = date(2026, 8, 1);
        Date end = date(2026, 8, 31);
        OrderTransferRecord record = new OrderTransferRecord();
        record.setOrderId("O1");
        record.setSourceId("S1");
        record.setSourceName("源工厂");
        record.setTargetId("T1");
        record.setTargetName("目标工厂");
        record.setTargetOrderItemId("TI1");
        when(recordService.findAllTransferRecords("S1", null, start, end)).thenReturn(List.of(record));
        OrderItem item = new OrderItem();
        item.setOrderId("O1");
        item.setOrderItemId("TI1");
        when(itemService.filterListUrgentFirst(eq(1), eq(20), any(Map.class))).thenReturn(List.of(item));
        when(itemService.filterTotal(any(Map.class))).thenReturn(1L);
        when(orderInfoService.findByOrderIds(any())).thenReturn(List.of());
        TransferDailyStatistics totals = new TransferDailyStatistics();
        totals.setTotalOrderCount(2L);
        totals.setTotalAmount(new BigDecimal("88.50"));
        when(statisticsService.sum("S1", null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(totals);

        TransferOrderStatisticsVO result = service.findTransferOrderStatistics(
                "S1", null, start, end, new PagedQuery(1, 20));

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getSourceId()).isEqualTo("S1");
        assertThat(result.getItems().get(0).getTargetId()).isEqualTo("T1");
        assertThat(result.getItems().get(0).getTargetName()).isEqualTo("目标工厂");
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getTotalOrderCount()).isEqualTo(2);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("88.50");
        verify(statisticsService).sum("S1", null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
    }

    @Test
    void shouldReturnAllTransferredItemsWithoutPagination() {
        AppOrderService service = new AppOrderService();
        OrderTransferRecordService recordService = mock(OrderTransferRecordService.class);
        OrderItemService itemService = mock(OrderItemService.class);
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        TransferDailyStatisticsService statisticsService = mock(TransferDailyStatisticsService.class);
        ReflectionTestUtils.setField(service, "orderTransferRecordService", recordService);
        ReflectionTestUtils.setField(service, "domainOrderItemService", itemService);
        ReflectionTestUtils.setField(service, "domainOrderInfoService", orderInfoService);
        ReflectionTestUtils.setField(service, "transferDailyStatisticsService", statisticsService);

        Date start = date(2026, 8, 1);
        Date end = date(2026, 8, 31);
        OrderTransferRecord record = new OrderTransferRecord();
        record.setOrderId("O1");
        record.setSourceId("S1");
        record.setSourceName("源工厂");
        record.setTargetId("T1");
        record.setTargetName("目标工厂");
        record.setTargetOrderItemId("TI1");
        when(recordService.findAllTransferRecords("S1", "T1", start, end)).thenReturn(List.of(record));
        OrderItem item = new OrderItem();
        item.setOrderId("O1");
        item.setOrderItemId("TI1");
        when(itemService.filterAllUrgentFirst(any(Map.class))).thenReturn(List.of(item));
        when(orderInfoService.findByOrderIds(any())).thenReturn(List.of());
        TransferDailyStatistics totals = new TransferDailyStatistics();
        totals.setTotalOrderCount(1L);
        totals.setTotalAmount(new BigDecimal("188.50"));
        when(statisticsService.sum("S1", "T1", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(totals);

        TransferOrderStatisticsVO result = service.findAllTransferOrderStatistics("S1", "T1", start, end);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getSourceName()).isEqualTo("源工厂");
        assertThat(result.getItems().get(0).getTargetName()).isEqualTo("目标工厂");
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getTotalOrderCount()).isEqualTo(1);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("188.50");
        verify(itemService).filterAllUrgentFirst(any(Map.class));
        verify(itemService, never()).filterListUrgentFirst(anyInt(), anyInt(), any(Map.class));
    }

    private Date date(int year, int month, int day) {
        return Date.from(LocalDate.of(year, month, day).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant());
    }

}

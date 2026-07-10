package com.mes.infra.dal.order.orderStatistics.po;

import com.mes.domain.order.orderStatistics.entity.OrderDailyStatistics;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "orderDailyStatistics")
public class OrderDailyStatisticsPo extends BasePO<OrderDailyStatistics> {
    private String manufacturerMetaId;
    private LocalDate statisticsDate;
    private Long totalOrderCount;
    private BigDecimal totalArea;
    private BigDecimal totalAmount;

    @Override
    public OrderDailyStatistics toDO() {
        OrderDailyStatistics statistics = new OrderDailyStatistics();
        copyBaseFieldsToDO(statistics);
        statistics.setManufacturerMetaId(manufacturerMetaId);
        statistics.setStatisticsDate(statisticsDate);
        statistics.setTotalOrderCount(totalOrderCount);
        statistics.setTotalArea(totalArea);
        statistics.setTotalAmount(totalAmount);
        return statistics;
    }

    @Override
    protected BasePO<OrderDailyStatistics> fromDO(OrderDailyStatistics statistics) {
        this.manufacturerMetaId = statistics.getManufacturerMetaId();
        this.statisticsDate = statistics.getStatisticsDate();
        this.totalOrderCount = statistics.getTotalOrderCount();
        this.totalArea = statistics.getTotalArea();
        this.totalAmount = statistics.getTotalAmount();
        return this;
    }
}

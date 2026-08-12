package com.mes.infra.dal.order.orderStatistics.po;

import com.mes.domain.order.orderStatistics.entity.OrderDailyStatistics;
import com.mes.domain.order.orderStatistics.entity.OrderStatisticsType;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.math.BigDecimal;
import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "orderDailyStatistics")
@CompoundIndex(name = "uk_order_daily_dimension", def = "{'manufacturerMetaId':1,'statisticsDate':1,'indexId':1,'type':1}", unique = true)
public class OrderDailyStatisticsPo extends BasePO<OrderDailyStatistics> {
    private String manufacturerMetaId;
    private LocalDate statisticsDate;
    private String indexId;
    private OrderStatisticsType type;
    private Long totalOrderCount;
    private BigDecimal totalArea;
    private BigDecimal totalAmount;

    @Override
    public OrderDailyStatistics toDO() {
        OrderDailyStatistics statistics = new OrderDailyStatistics();
        copyBaseFieldsToDO(statistics);
        statistics.setManufacturerMetaId(manufacturerMetaId);
        statistics.setStatisticsDate(statisticsDate);
        statistics.setIndexId(indexId);
        statistics.setType(type);
        statistics.setTotalOrderCount(totalOrderCount);
        statistics.setTotalArea(totalArea);
        statistics.setTotalAmount(totalAmount);
        return statistics;
    }

    @Override
    protected BasePO<OrderDailyStatistics> fromDO(OrderDailyStatistics statistics) {
        this.manufacturerMetaId = statistics.getManufacturerMetaId();
        this.statisticsDate = statistics.getStatisticsDate();
        this.indexId = statistics.getIndexId();
        this.type = statistics.getType();
        this.totalOrderCount = statistics.getTotalOrderCount();
        this.totalArea = statistics.getTotalArea();
        this.totalAmount = statistics.getTotalAmount();
        return this;
    }
}

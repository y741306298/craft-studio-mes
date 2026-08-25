package com.mes.infra.dal.order.transferStatistics.po;

import com.mes.domain.order.transferStatistics.entity.TransferDailyStatistics;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "transferDailyStatistics")
@CompoundIndex(name = "uk_transfer_daily_route", def = "{'sourceId':1,'targetId':1,'statisticsDate':1}", unique = true)
public class TransferDailyStatisticsPo extends BasePO<TransferDailyStatistics> {
    private String sourceId;
    private String targetId;
    private String targetName;
    private LocalDate statisticsDate;
    private Long totalOrderCount;
    private BigDecimal totalAmount;

    @Override
    public TransferDailyStatistics toDO() {
        TransferDailyStatistics statistics = new TransferDailyStatistics();
        copyBaseFieldsToDO(statistics);
        statistics.setSourceId(sourceId);
        statistics.setTargetId(targetId);
        statistics.setTargetName(targetName);
        statistics.setStatisticsDate(statisticsDate);
        statistics.setTotalOrderCount(totalOrderCount);
        statistics.setTotalAmount(totalAmount);
        return statistics;
    }

    @Override
    protected BasePO<TransferDailyStatistics> fromDO(TransferDailyStatistics statistics) {
        sourceId = statistics.getSourceId();
        targetId = statistics.getTargetId();
        targetName = statistics.getTargetName();
        statisticsDate = statistics.getStatisticsDate();
        totalOrderCount = statistics.getTotalOrderCount();
        totalAmount = statistics.getTotalAmount();
        return this;
    }
}

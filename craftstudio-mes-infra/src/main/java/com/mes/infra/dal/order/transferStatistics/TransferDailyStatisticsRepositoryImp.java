package com.mes.infra.dal.order.transferStatistics;

import com.mes.domain.order.transferStatistics.entity.TransferDailyStatistics;
import com.mes.domain.order.transferStatistics.repository.TransferDailyStatisticsRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.order.transferStatistics.po.TransferDailyStatisticsPo;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Date;

@Repository
public class TransferDailyStatisticsRepositoryImp
        extends BaseRepositoryImp<TransferDailyStatistics, TransferDailyStatisticsPo>
        implements TransferDailyStatisticsRepository {

    @Override
    public Class<TransferDailyStatisticsPo> poClass() {
        return TransferDailyStatisticsPo.class;
    }

    @Override
    public TransferDailyStatistics increment(String sourceId, String targetId, String targetName,
                                             LocalDate statisticsDate, long orderCount, BigDecimal amount) {
        Date now = new Date();
        Query query = new SoftDeleteQuery(Criteria.where("sourceId").is(sourceId)
                .and("targetId").is(targetId).and("statisticsDate").is(statisticsDate));
        Update update = new Update()
                .setOnInsert("sourceId", sourceId)
                .setOnInsert("targetId", targetId)
                .set("targetName", targetName)
                .setOnInsert("statisticsDate", statisticsDate)
                .setOnInsert("createTime", now)
                .set("updateTime", now)
                .inc("totalOrderCount", orderCount)
                .inc("totalAmount", scale(amount));
        TransferDailyStatisticsPo po = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().upsert(true).returnNew(true), poClass());
        return po == null ? null : po.toDO();
    }

    @Override
    public TransferDailyStatistics sum(String sourceId, String targetId,
                                       LocalDate startDate, LocalDate endDate) {
        Criteria criteria = Criteria.where("sourceId").is(sourceId)
                .and("targetId").is(targetId)
                .and("statisticsDate").gte(startDate).lte(endDate)
                .and("deleteAt").is(null);
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group()
                        .sum("totalOrderCount").as("totalOrderCount")
                        .sum("totalAmount").as("totalAmount"));
        AggregationResults<TransferDailyStatisticsPo> results = mongoTemplate.aggregate(
                aggregation, mongoTemplate.getCollectionName(poClass()), poClass());
        TransferDailyStatisticsPo po = results.getUniqueMappedResult();
        if (po == null || po.getTotalOrderCount() == null) {
            return null;
        }
        TransferDailyStatistics statistics = po.toDO();
        statistics.setSourceId(sourceId);
        statistics.setTargetId(targetId);
        statistics.setStatisticsDate(startDate);
        statistics.setTotalAmount(scale(statistics.getTotalAmount()));
        return statistics;
    }

    private BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}

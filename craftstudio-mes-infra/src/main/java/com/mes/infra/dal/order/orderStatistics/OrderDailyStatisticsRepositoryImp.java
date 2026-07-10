package com.mes.infra.dal.order.orderStatistics;

import com.mes.domain.order.orderStatistics.entity.OrderDailyStatistics;
import com.mes.domain.order.orderStatistics.repository.OrderDailyStatisticsRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.order.orderStatistics.po.OrderDailyStatisticsPo;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Repository
public class OrderDailyStatisticsRepositoryImp extends BaseRepositoryImp<OrderDailyStatistics, OrderDailyStatisticsPo>
        implements OrderDailyStatisticsRepository {
    @Override
    public Class<OrderDailyStatisticsPo> poClass() {
        return OrderDailyStatisticsPo.class;
    }

    @Override
    public OrderDailyStatistics findByManufacturerMetaIdAndStatisticsDate(String manufacturerMetaId, LocalDate statisticsDate) {
        OrderDailyStatisticsPo po = mongoTemplate.findOne(
                new SoftDeleteQuery(Criteria.where("manufacturerMetaId").is(manufacturerMetaId)
                        .and("statisticsDate").is(statisticsDate)),
                poClass()
        );
        return po == null ? null : po.toDO();
    }

    @Override
    public OrderDailyStatistics increment(String manufacturerMetaId,
                                          LocalDate statisticsDate,
                                          long orderCount,
                                          BigDecimal area,
                                          BigDecimal amount) {
        Date now = new Date();
        Query query = new SoftDeleteQuery(Criteria.where("manufacturerMetaId").is(manufacturerMetaId)
                .and("statisticsDate").is(statisticsDate));
        Update update = new Update()
                .setOnInsert("manufacturerMetaId", manufacturerMetaId)
                .setOnInsert("statisticsDate", statisticsDate)
                .setOnInsert("createTime", now)
                .set("updateTime", now)
                .inc("totalOrderCount", orderCount)
                .inc("totalArea", area == null ? BigDecimal.ZERO : area)
                .inc("totalAmount", amount == null ? BigDecimal.ZERO : amount);
        OrderDailyStatisticsPo po = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                poClass()
        );
        return po == null ? null : po.toDO();
    }
}

package com.mes.infra.dal.order;

import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.repository.OrderItemRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.order.po.OrderItemPo;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class OrderItemRepositoryImp extends BaseRepositoryImp<OrderItem, OrderItemPo> implements OrderItemRepository {

    @Override
    public Class<OrderItemPo> poClass() {
        return OrderItemPo.class;
    }

    @Override
    public List<OrderItem> findByOrderItemIds(Collection<String> orderItemIds) {
        long start = System.nanoTime();
        List<OrderItemPo> pos = mongoTemplate.find(
                new Query(new Criteria().andOperator(
                        Criteria.where("deleteAt").is(null),
                        new Criteria().orOperator(
                                Criteria.where("orderItemId").in(orderItemIds),
                                Criteria.where("_id").in(orderItemIds)))), poClass());
        log.info("MongoDB query findByOrderItemIds completed: ids={}, results={}, elapsedMs={}",
                orderItemIds.size(), pos.size(), (System.nanoTime() - start) / 1_000_000.0);
        return pos.stream().map(OrderItemPo::toDO).toList();
    }

    @Override
    public List<OrderItem> filterListUrgentFirst(long current, int size, Map<String, Object> filters) {
        return filterList(
                current,
                size,
                filters,
                Sort.by(
                        Sort.Order.desc("isUrgent"),
                        Sort.Order.desc("updateTime")
                )
        );
    }

    @Override
    public List<OrderItem> filterAllUrgentFirst(Map<String, Object> filters) {
        Query query = new SoftDeleteQuery(buildFilterCriteria(filters)).with(
                Sort.by(
                        Sort.Order.desc("isUrgent"),
                        Sort.Order.desc("updateTime")
                ));
        return mongoTemplate.find(query, poClass()).stream().map(OrderItemPo::toDO).toList();
    }
}

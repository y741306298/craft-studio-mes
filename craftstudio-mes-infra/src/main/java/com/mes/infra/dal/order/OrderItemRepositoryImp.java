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
import org.springframework.data.mongodb.core.query.Update;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Date;

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
    public List<OrderItem> findByOrderIdAndOrderItemIds(String orderId, Collection<String> orderItemIds) {
        if (orderId == null || orderItemIds == null || orderItemIds.isEmpty()) {
            return List.of();
        }
        return mongoTemplate.find(
                new SoftDeleteQuery(new Criteria().andOperator(
                        Criteria.where("orderId").is(orderId),
                        Criteria.where("orderItemId").in(orderItemIds))),
                poClass()).stream().map(OrderItemPo::toDO).toList();
    }

    @Override
    public List<OrderItem> findByOrderIds(Collection<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        return mongoTemplate.find(
                new SoftDeleteQuery(Criteria.where("orderId").in(orderIds)), poClass())
                .stream().map(OrderItemPo::toDO).toList();
    }

    @Override
    public List<OrderItem> findAllByOrderId(String orderId) {
        return mongoTemplate.find(
                new SoftDeleteQuery(Criteria.where("orderId").is(orderId))
                        .with(Sort.by(Sort.Order.asc("createTime"), Sort.Order.asc("_id"))),
                poClass()).stream().map(OrderItemPo::toDO).toList();
    }

    @Override
    public long deleteByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        Date now = new Date();
        return mongoTemplate.updateMulti(
                new SoftDeleteQuery(Criteria.where("_id").in(ids)),
                new Update().set(SoftDeleteQuery.DELETED_AT, now).set("updateTime", now),
                poClass()).getModifiedCount();
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

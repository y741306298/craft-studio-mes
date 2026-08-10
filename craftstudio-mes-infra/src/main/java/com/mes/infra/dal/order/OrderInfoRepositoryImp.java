package com.mes.infra.dal.order;

import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.repository.OrderInfoRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.order.po.OrderInfoPo;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;

@Repository
@Slf4j
public class OrderInfoRepositoryImp extends BaseRepositoryImp<OrderInfo, OrderInfoPo> implements OrderInfoRepository {

    @Override
    public Class<OrderInfoPo> poClass() {
        return OrderInfoPo.class;
    }

    @Override
    public List<OrderInfo> findByOrderIds(Collection<String> orderIds) {
        long start = System.nanoTime();
        List<OrderInfoPo> pos = mongoTemplate.find(
                new Query(Criteria.where("orderId").in(orderIds).and("deleteAt").is(null)), poClass());
        log.info("MongoDB query findByOrderIds completed: ids={}, results={}, elapsedMs={}",
                orderIds.size(), pos.size(), (System.nanoTime() - start) / 1_000_000.0);
        return pos.stream().map(OrderInfoPo::toDO).toList();
    }
}

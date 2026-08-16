package com.mes.infra.dal.order;

import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.repository.OrderInfoRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.order.po.OrderInfoPo;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Repository
@Slf4j
public class OrderInfoRepositoryImp extends BaseRepositoryImp<OrderInfo, OrderInfoPo> implements OrderInfoRepository {

    private static final String TRANSFER_LOCK_TOKEN = "transferLockToken";
    private static final String TRANSFER_LOCK_TIME = "transferLockTime";

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

    @Override
    public boolean tryAcquireTransferLock(String id, String lockToken, Date expiredBefore) {
        Criteria unlockedOrExpired = new Criteria().orOperator(
                Criteria.where(TRANSFER_LOCK_TOKEN).exists(false),
                Criteria.where(TRANSFER_LOCK_TOKEN).is(null),
                Criteria.where(TRANSFER_LOCK_TIME).lt(expiredBefore));
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("_id").is(id),
                Criteria.where("deleteAt").is(null),
                unlockedOrExpired));
        Update update = new Update()
                .set(TRANSFER_LOCK_TOKEN, lockToken)
                .set(TRANSFER_LOCK_TIME, new Date());
        return mongoTemplate.updateFirst(query, update, poClass()).getModifiedCount() == 1;
    }

    @Override
    public void releaseTransferLock(String id, String lockToken) {
        Query query = new Query(Criteria.where("_id").is(id).and(TRANSFER_LOCK_TOKEN).is(lockToken));
        Update update = new Update().unset(TRANSFER_LOCK_TOKEN).unset(TRANSFER_LOCK_TIME);
        mongoTemplate.updateFirst(query, update, poClass());
    }
}

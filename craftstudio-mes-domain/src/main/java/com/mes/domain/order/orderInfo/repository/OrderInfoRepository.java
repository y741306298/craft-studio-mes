package com.mes.domain.order.orderInfo.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.order.orderInfo.entity.OrderInfo;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface OrderInfoRepository extends BaseRepository<OrderInfo> {

    List<OrderInfo> findByOrderIds(Collection<String> orderIds);

    boolean tryAcquireTransferLock(String id, String lockToken, Date expiredBefore);

    void releaseTransferLock(String id, String lockToken);

}

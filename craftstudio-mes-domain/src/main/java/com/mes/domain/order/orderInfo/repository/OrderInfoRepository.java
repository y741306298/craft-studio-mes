package com.mes.domain.order.orderInfo.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.order.orderInfo.entity.OrderInfo;

import java.util.Collection;
import java.util.List;

public interface OrderInfoRepository extends BaseRepository<OrderInfo> {

    List<OrderInfo> findByOrderIds(Collection<String> orderIds);

}

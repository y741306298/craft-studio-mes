package com.mes.domain.order.orderInfo.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.order.orderInfo.entity.OrderItem;

import java.util.List;
import java.util.Map;
import java.util.Collection;

public interface OrderItemRepository extends BaseRepository<OrderItem> {

    List<OrderItem> findByOrderItemIds(Collection<String> orderItemIds);

    /**
     * 根据条件过滤订单项列表，并优先返回加急订单项。
     *
     * @param current 当前页码
     * @param size 每页大小
     * @param filters 过滤条件
     * @return 订单项列表
     */
    List<OrderItem> filterListUrgentFirst(long current, int size, Map<String, Object> filters);
}

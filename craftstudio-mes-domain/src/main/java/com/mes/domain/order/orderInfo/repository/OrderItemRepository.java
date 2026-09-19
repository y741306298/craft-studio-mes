package com.mes.domain.order.orderInfo.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.order.orderInfo.entity.OrderItem;

import java.util.List;
import java.util.Map;
import java.util.Collection;

public interface OrderItemRepository extends BaseRepository<OrderItem> {

    List<OrderItem> findByOrderItemIds(Collection<String> orderItemIds);

    /**
     * 批量查询指定订单下可能已存在的订单项，供订单导入时一次性完成去重检查。
     */
    List<OrderItem> findByOrderIdAndOrderItemIds(String orderId, Collection<String> orderItemIds);

    List<OrderItem> findByOrderIds(Collection<String> orderIds);

    /** 按创建时间正序查询指定订单的全部有效订单项。 */
    List<OrderItem> findAllByOrderId(String orderId);

    /** 批量软删除订单项。 */
    long deleteByIds(Collection<String> ids);

    /**
     * 根据条件过滤订单项列表，并优先返回加急订单项。
     *
     * @param current 当前页码
     * @param size 每页大小
     * @param filters 过滤条件
     * @return 订单项列表
     */
    List<OrderItem> filterListUrgentFirst(long current, int size, Map<String, Object> filters);

    /** 根据条件全量查询订单项，不应用分页限制，并优先返回加急订单项。 */
    List<OrderItem> filterAllUrgentFirst(Map<String, Object> filters);
}

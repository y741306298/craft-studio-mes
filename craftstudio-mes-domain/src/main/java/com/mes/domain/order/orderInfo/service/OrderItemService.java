package com.mes.domain.order.orderInfo.service;

import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.repository.OrderItemRepository;
import com.mes.domain.shared.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderItemService {

    @Autowired
    private OrderItemRepository orderItemRepository;

    /**
     * 根据 ID 获取订单项
     * @param id 订单项 ID
     * @return 订单项实体
     */
    public OrderItem findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException("订单项 ID 不能为空");
        }
        return orderItemRepository.findById(id);
    }

    /**
     * 根据订单 ID 查询订单项列表（支持分页）
     * @param orderId 订单 ID
     * @param current 当前页码
     * @param size 每页大小
     * @return 订单项列表
     */
    public java.util.List<OrderItem> findByOrderId(String orderId, int current, int size) {

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException("每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(orderId)) {
            throw new BusinessNotAllowException("订单 ID 不能为空");
        }

        java.util.Map<String, Object> filters = new java.util.HashMap<>();
        filters.put("orderId", orderId);
        return orderItemRepository.filterList(current, size, filters);
    }

    /**
     * 添加订单项
     * @param orderItem 订单项实体
     * @return 添加后的实体
     */
    public OrderItem addOrderItem(OrderItem orderItem) {
        if (orderItem == null) {
            throw new BusinessNotAllowException("订单项不能为空");
        }
        if (StringUtils.isBlank(orderItem.getOrderItemId())) {
            throw new BusinessNotAllowException("订单项 ID 不能为空");
        }
        if (StringUtils.isBlank(orderItem.getOrderId())) {
            throw new BusinessNotAllowException("订单 ID 不能为空");
        }

        return orderItemRepository.add(orderItem);
    }

    /**
     * 更新订单项
     * @param orderItem 订单项实体
     */
    public void updateOrderItem(OrderItem orderItem) {
        if (orderItem == null) {
            throw new BusinessNotAllowException("订单项不能为空");
        }
        if (StringUtils.isBlank(orderItem.getId())) {
            throw new BusinessNotAllowException("订单项 ID 不能为空");
        }

        orderItemRepository.update(orderItem);
    }

    /**
     * 删除订单项
     * @param id 订单项 ID
     */
    public void deleteOrderItem(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException("订单项 ID 不能为空");
        }

        OrderItem orderItem = findById(id);
        if (orderItem != null) {
            orderItemRepository.delete(orderItem);
        }
    }

    /**
     * 标记订单项为失败状态
     * @param orderItemId 订单项 ID
     * @param reason 失败原因
     */
    public void markAsFailed(String orderItemId, String reason) {
        if (StringUtils.isBlank(orderItemId)) {
            throw new BusinessNotAllowException("订单项 ID 不能为空");
        }

        OrderItem orderItem = findById(orderItemId);
        if (orderItem != null) {
            orderItem.setStatus(OrderStatus.FAILED);
            orderItem.setFailureReason(reason);
            orderItemRepository.update(orderItem);
        }
    }
}

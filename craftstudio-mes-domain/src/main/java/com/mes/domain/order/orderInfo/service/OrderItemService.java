package com.mes.domain.order.orderInfo.service;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.FlowStatus;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.repository.OrderItemRepository;
import com.mes.domain.shared.exception.BusinessNotAllowException;
import com.mes.domain.shared.util.IdGenerator;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * 根据 orderItemId 获取订单项
     * @param orderItemId 订单项业务 ID
     * @return 订单项实体
     */
    public OrderItem findByOrderItemId(String orderItemId) {
        if (StringUtils.isBlank(orderItemId)) {
            throw new BusinessNotAllowException("订单项业务 ID 不能为空");
        }

        java.util.Map<String, Object> filters = new java.util.HashMap<>();
        filters.put("orderItemId", orderItemId);
        java.util.List<OrderItem> items = orderItemRepository.filterList(1, 1, filters);
        
        if (items == null || items.isEmpty()) {
            return null;
        }
        
        return items.get(0);
    }

    /**
     * 根据订单 ID 查询订单项列表（支持分页）
     * @param orderId 订单 ID
     * @param current 当前页码
     * @param size 每页大小
     * @return 订单项列表
     */
    public java.util.List<OrderItem> findByOrderId(String orderId,String manufacturerId, int current, int size) {

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException("每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(orderId)) {
            throw new BusinessNotAllowException("订单 ID 不能为空");
        }

        java.util.Map<String, Object> filters = new java.util.HashMap<>();
        filters.put("orderId", orderId);
        filters.put("manufacturerId", manufacturerId);
        return orderItemRepository.filterList(current, size, filters);
    }

    /**
     * 根据条件过滤订单项列表（支持分页）
     * @param current 当前页码
     * @param size 每页大小
     * @param filters 过滤条件
     * @return 订单项列表
     */
    public java.util.List<OrderItem> filterList(int current, int size, Map<String, Object> filters) {
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException("每页大小必须在 1-100 之间");
        }
        return orderItemRepository.filterList(current, size, filters);
    }

    /**
     * 根据条件统计订单项数量
     * @param filters 过滤条件
     * @return 订单项数量
     */
    public long filterTotal(Map<String, Object> filters) {
        return orderItemRepository.filterTotal(filters);
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

        // 如果没有设置 orderItemId，则生成一个
        if (StringUtils.isBlank(orderItem.getOrderItemId())) {
            String orderItemId = IdGenerator.generateOrderItemId();
            orderItem.setOrderItemId(orderItemId);
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
     * @param reason 失败原因
     */
    public void markAsFailed(String id, String reason) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException("订单项 ID 不能为空");
        }

        OrderItem orderItem = findById(id);
        if (orderItem != null) {
            orderItem.setStatus(OrderStatus.FAILED);
            orderItem.setFailureReason(reason);
            orderItemRepository.update(orderItem);
        }
    }



}

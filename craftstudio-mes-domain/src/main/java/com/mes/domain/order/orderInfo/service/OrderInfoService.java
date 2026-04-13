package com.mes.domain.order.orderInfo.service;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.repository.OrderInfoRepository;
import com.mes.domain.order.orderInfo.repository.OrderItemRepository;
import com.mes.domain.shared.exception.BusinessNotAllowException;
import com.mes.domain.shared.utils.IdGenerator;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderInfoService {

    @Autowired
    private OrderInfoRepository orderInfoRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderPreprocessingService orderPreprocessingService;

    /**
     * 根据 ID 获取订单信息
     * @param id 订单 ID
     * @return 订单信息实体
     */
    public OrderInfo findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException("订单 ID 不能为空");
        }
        return orderInfoRepository.findById(id);
    }

    /**
     * 根据订单号查询订单信息
     * @param orderId 订单号
     * @return 订单信息实体
     */
    public OrderInfo findByOrderId(String orderId) {
        if (StringUtils.isBlank(orderId)) {
            throw new BusinessNotAllowException("订单号不能为空");
        }

        Map<String, Object> filters = new HashMap<>();
        filters.put("orderId", orderId);
        List<OrderInfo> results = orderInfoRepository.filterList(1, 1, filters);

        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 查询订单列表（支持分页）
     * @param current 当前页码
     * @param size 每页大小
     * @return 订单列表
     */
    public List<OrderInfo> listOrders(int current, int size) {
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException("每页大小必须在 1-100 之间");
        }
        return orderInfoRepository.list(current, size);
    }

    /**
     * 根据状态查询订单列表（支持分页）
     * @param status 订单状态
     * @param current 当前页码
     * @param size 每页大小
     * @return 订单列表
     */
    public List<OrderInfo> findOrdersByStatus(String status, int current, int size) {
        if (StringUtils.isBlank(status)) {
            throw new BusinessNotAllowException("订单状态不能为空");
        }
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException("每页大小必须在 1-100 之间");
        }

        Map<String, Object> filters = new HashMap<>();
        filters.put("status", status);
        return orderInfoRepository.filterList(current, size, filters);
    }

    /**
     * 模糊搜索订单（支持分页）
     * @param orderId 订单号（支持模糊匹配）
     * @param current 当前页码
     * @param size 每页大小
     * @return 订单列表
     */
    public List<OrderInfo> searchOrders(String orderId, int current, int size) {
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException("每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(orderId)) {
            throw new BusinessNotAllowException("订单号不能为空");
        }

        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("orderId", orderId);
        return orderInfoRepository.fuzzySearch(searchFilters, current, size);
    }

    /**
     * 添加订单
     * @param orderInfo 订单信息实体
     * @return 添加后的实体
     */
    public OrderInfo addOrder(OrderInfo orderInfo) {
        if (orderInfo == null) {
            throw new BusinessNotAllowException("订单信息不能为空");
        }
        if (StringUtils.isBlank(orderInfo.getOrderId())) {
            throw new BusinessNotAllowException("订单号不能为空");
        }

        return orderInfoRepository.add(orderInfo);
    }

    /**
     * 更新订单
     * @param orderInfo 订单信息实体
     */
    public void updateOrder(OrderInfo orderInfo) {
        if (orderInfo == null) {
            throw new BusinessNotAllowException("订单信息不能为空");
        }
        if (StringUtils.isBlank(orderInfo.getId())) {
            throw new BusinessNotAllowException("订单 ID 不能为空");
        }

        orderInfoRepository.update(orderInfo);
    }

    /**
     * 删除订单
     * @param id 订单 ID
     */
    public void deleteOrder(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException("订单 ID 不能为空");
        }

        OrderInfo orderInfo = findById(id);
        if (orderInfo != null) {
            orderInfoRepository.delete(orderInfo);
        }
    }

    /**
     * 批量添加订单
     * @param orders 订单列表
     * @return 添加后的订单列表
     */
    public List<OrderInfo> batchAddOrders(List<OrderInfo> orders) {
        if (orders == null || orders.isEmpty()) {
            throw new BusinessNotAllowException("订单列表不能为空");
        }
        return (List<OrderInfo>) orderInfoRepository.batchAdd(orders);
    }

    /**
     * 批量更新订单
     * @param orders 订单列表
     */
    public void batchUpdateOrders(List<OrderInfo> orders) {
        if (orders == null || orders.isEmpty()) {
            throw new BusinessNotAllowException("订单列表不能为空");
        }
        orderInfoRepository.batchUpdate(orders);
    }

    /**
     * 获取订单总数
     * @return 订单总数
     */
    public long getTotalCount() {
        return orderInfoRepository.total();
    }

    /**
     * 根据状态统计订单数量
     * @param status 订单状态
     * @return 订单数量
     */
    public long countByStatus(String status) {
        if (StringUtils.isBlank(status)) {
            throw new BusinessNotAllowException("订单状态不能为空");
        }

        Map<String, Object> filters = new HashMap<>();
        filters.put("status", status);
        return orderInfoRepository.filterTotal(filters);
    }

    /**
     * 检查订单是否存在
     * @param id 订单 ID
     * @return 是否存在
     */
    public boolean existById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException("订单 ID 不能为空");
        }
        return orderInfoRepository.existById(id);
    }

    /**
     * 根据订单号检查订单是否存在
     * @param orderId 订单号
     * @return 是否存在
     */
    public boolean existByOrderId(String orderId) {
        OrderInfo order = findByOrderId(orderId);
        return order != null;
    }

    /**
     * 根据多条件分页查询订单列表
     * @param orderId 订单号（可为 null）
     * @param status 订单状态（可为 null）
     * @param startTime 开始时间（可为 null）
     * @param endTime 结束时间（可为 null）
     * @param current 当前页码
     * @param size 每页大小
     * @return 订单列表
     */
    public List<OrderInfo> findOrdersByConditions(String orderId, String status, Date startTime, Date endTime, int current, int size) {
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException("每页大小必须在 1-100 之间");
        }

        Map<String, Object> filters = new HashMap<>();
        if (StringUtils.isNotBlank(orderId)) {
            filters.put("orderId", orderId);
        }
        if (StringUtils.isNotBlank(status)) {
            filters.put("status", status);
        }
        if (startTime != null) {
            filters.put("createTime_gte", startTime);
        }
        if (endTime != null) {
            filters.put("createTime_lte", endTime);
        }

        if (filters.isEmpty()) {
            return orderInfoRepository.list(current, size);
        }

        return orderInfoRepository.filterList(current, size, filters);
    }

    /**
     * 根据多条件查询订单（包含客户手机号）
     * @param orderId 订单号
     * @param status 订单状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param customerPhone 客户手机号
     * @param current 当前页码
     * @param size 每页大小
     * @return 订单列表
     */
    public List<OrderInfo> findOrdersByConditionsWithCustomerPhone(String orderId, String status, Date startTime, Date endTime, String customerPhone, int current, int size) {
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException("每页大小必须在 1-100 之间");
        }

        Map<String, Object> filters = new HashMap<>();
        if (StringUtils.isNotBlank(orderId)) {
            filters.put("orderId", orderId);
        }
        if (StringUtils.isNotBlank(status)) {
            filters.put("status", status);
        }
        if (startTime != null) {
            filters.put("createTime_gte", startTime);
        }
        if (endTime != null) {
            filters.put("createTime_lte", endTime);
        }
        if (StringUtils.isNotBlank(customerPhone)) {
            filters.put("customer.customerPhone", customerPhone);
        }

        if (filters.isEmpty()) {
            return orderInfoRepository.list(current, size);
        }

        return orderInfoRepository.filterList(current, size, filters);
    }

    /**
     * 根据多条件统计订单数量
     * @param orderId 订单号（可为 null）
     * @param status 订单状态（可为 null）
     * @param startTime 开始时间（可为 null）
     * @param endTime 结束时间（可为 null）
     * @return 订单数量
     */
    public long countByConditions(String orderId, String status, Date startTime, Date endTime) {
        Map<String, Object> filters = new HashMap<>();
        if (StringUtils.isNotBlank(orderId)) {
            filters.put("orderId", orderId);
        }
        if (StringUtils.isNotBlank(status)) {
            filters.put("status", status);
        }
        if (startTime != null) {
            filters.put("createTime_gte", startTime);
        }
        if (endTime != null) {
            filters.put("createTime_lte", endTime);
        }

        if (filters.isEmpty()) {
            return orderInfoRepository.total();
        }

        return orderInfoRepository.filterTotal(filters);
    }

    /**
     * 统计符合条件的订单数量（包含客户手机号）
     * @param orderId 订单号
     * @param status 订单状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param customerPhone 客户手机号
     * @return 订单总数
     */
    public long countByConditionsWithCustomerPhone(String orderId, String status, Date startTime, Date endTime, String customerPhone) {
        Map<String, Object> filters = new HashMap<>();
        if (StringUtils.isNotBlank(orderId)) {
            filters.put("orderId", orderId);
        }
        if (StringUtils.isNotBlank(status)) {
            filters.put("status", status);
        }
        if (startTime != null) {
            filters.put("createTime_gte", startTime);
        }
        if (endTime != null) {
            filters.put("createTime_lte", endTime);
        }
        if (StringUtils.isNotBlank(customerPhone)) {
            filters.put("customer.customerPhone", customerPhone);
        }

        if (filters.isEmpty()) {
            return orderInfoRepository.total();
        }

        return orderInfoRepository.filterTotal(filters);
    }

    /**
     * 添加订单及订单项
     * @param orderInfo 订单信息
     * @param orderItems 订单项列表
     * @return 添加后的订单信息
     */
    public List<OrderItem> addOrderWithItems(OrderInfo orderInfo, List<OrderItem> orderItems) {
        
        // 检查订单是否已存在
        OrderInfo existingOrder = findByOrderId(orderInfo.getOrderId());

        // 为每个订单项生成唯一的 orderItemId 并完善 procedureFlow 数据
        for (OrderItem item : orderItems) {
            if (StringUtils.isBlank(item.getOrderItemId())) {
                String orderItemId = IdGenerator.generateOrderItemId();
                item.setOrderItemId(orderItemId);
            }
            item.setOrderId(orderInfo.getOrderId());

            // 从 mtoProduct 中获取 processFlow 并转换为 procedureFlow
            String processFlow = "";
            if (item.getMtoProduct() != null) {
                ProcedureFlow procedureFlow = orderPreprocessingService.convertProcessFlowToProcedureFlow(item.getMtoProduct());
                MaterialConfig materialConfigFromMTOProduct = this.orderPreprocessingService.getMaterialConfigFromMTOProduct(item.getMtoProduct());
                if (procedureFlow != null) {
                    List<ProcedureFlowNode> nodes = procedureFlow.getNodes();
                    for (int i = 0; i < nodes.size(); i++) {
                        ProcedureFlowNode node = nodes.get(i);
                        processFlow = processFlow + node.getNodeName();
                        if (i < nodes.size() - 1) {
                            processFlow = processFlow + "-";
                        }
                    }

                    item.setProcedureFlow(procedureFlow);
                }
                item.setMaterial(materialConfigFromMTOProduct);
            }
            item.setProcessingFlow(processFlow);
        }

        // 先添加订单主表
        OrderInfo savedOrder = orderInfoRepository.add(orderInfo);

        // 批量添加订单项
        orderItemRepository.batchAdd(orderItems);

        return orderItems;
    }
}

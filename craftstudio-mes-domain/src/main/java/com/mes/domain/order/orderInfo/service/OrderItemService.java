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

    /**
     * 将 MTOProductSpecDTO 中的 ProcessFlowDTO 转换为 ProcedureFlow
     * @param mtoProduct MTO 产品规格 DTO
     * @return 转换后的 ProcedureFlow，如果 mtoProduct 为 null 或没有 processFlow 则返回 null
     */
    public ProcedureFlow convertProcessFlowToProcedureFlow(MTOProductSpecDTO mtoProduct) {
        if (mtoProduct == null) {
            return null;
        }

        // 从 MTOProductSpecDTO 中获取 processFlow
        Object processFlowDTO = getProcessFlowFromMTOProduct(mtoProduct);
        if (processFlowDTO == null) {
            return null;
        }

        // 创建 ProcedureFlow
        ProcedureFlow procedureFlow = new ProcedureFlow();
        
        // 设置基本信息
        procedureFlow.setProcedureFlowId(getProcessFlowId(processFlowDTO));
        procedureFlow.setProcedureFlowName(getProcessFlowName(processFlowDTO));
        procedureFlow.setFlowDescription(getProcessFlowDescription(processFlowDTO));
        procedureFlow.setFlowStatus(FlowStatus.NOT_STARTED);

        // 转换节点列表
        List<Object> nodes = getProcessFlowNodes(processFlowDTO);
        if (nodes != null && !nodes.isEmpty()) {
            List<ProcedureFlowNode> procedureFlowNodes = nodes.stream()
                    .map(this::convertProcessFlowNodeToProcedureFlowNode)
                    .collect(Collectors.toList());
            
            procedureFlow.setNodes(procedureFlowNodes);
            procedureFlow.setTotalNodes(procedureFlowNodes.size());
        } else {
            procedureFlow.setNodes(new ArrayList<>());
            procedureFlow.setTotalNodes(0);
        }

        return procedureFlow;
    }

    /**
     * 从 MTOProductSpecDTO 中获取 processFlow
     * 注意：此处需要使用反射或其他方式访问外部 DTO 的字段
     */
    private Object getProcessFlowFromMTOProduct(MTOProductSpecDTO mtoProduct) {
        try {
            java.lang.reflect.Method getProcessFlowMethod = mtoProduct.getClass().getDeclaredMethod("getProcessFlow");
            return getProcessFlowMethod.invoke(mtoProduct);
        } catch (Exception e) {
            throw new BusinessNotAllowException("无法获取 processFlow 信息：" + e.getMessage());
        }
    }

    /**
     * 获取 processFlow 的 ID
     */
    private String getProcessFlowId(Object processFlowDTO) {
        try {
            java.lang.reflect.Method getIdMethod = processFlowDTO.getClass().getDeclaredMethod("getId");
            return (String) getIdMethod.invoke(processFlowDTO);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 processFlow 的名称
     */
    private String getProcessFlowName(Object processFlowDTO) {
        try {
            java.lang.reflect.Method getNameMethod = processFlowDTO.getClass().getDeclaredMethod("getName");
            return (String) getNameMethod.invoke(processFlowDTO);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 processFlow 的描述
     */
    private String getProcessFlowDescription(Object processFlowDTO) {
        try {
            java.lang.reflect.Method getDescriptionMethod = processFlowDTO.getClass().getDeclaredMethod("getDescription");
            return (String) getDescriptionMethod.invoke(processFlowDTO);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 processFlow 的节点列表
     */
    @SuppressWarnings("unchecked")
    private List<Object> getProcessFlowNodes(Object processFlowDTO) {
        try {
            java.lang.reflect.Method getNodesMethod = processFlowDTO.getClass().getDeclaredMethod("getNodes");
            return (List<Object>) getNodesMethod.invoke(processFlowDTO);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 ProcessFlowNodeDTO 转换为 ProcedureFlowNode
     */
    private ProcedureFlowNode convertProcessFlowNodeToProcedureFlowNode(Object nodeDTO) {
        ProcedureFlowNode node = new ProcedureFlowNode();
        
        try {
            // 设置节点基本信息
            java.lang.reflect.Method getNodeIdMethod = nodeDTO.getClass().getDeclaredMethod("getNodeId");
            node.setNodeId((String) getNodeIdMethod.invoke(nodeDTO));
            
            java.lang.reflect.Method getNodeNameMethod = nodeDTO.getClass().getDeclaredMethod("getNodeName");
            node.setNodeName((String) getNodeNameMethod.invoke(nodeDTO));
            
            java.lang.reflect.Method getNodeOrderMethod = nodeDTO.getClass().getDeclaredMethod("getNodeOrder");
            Integer nodeOrder = (Integer) getNodeOrderMethod.invoke(nodeDTO);
            node.setNodeOrder(nodeOrder != null ? nodeOrder : 0);
            
            java.lang.reflect.Method getNodeTypeMethod = nodeDTO.getClass().getDeclaredMethod("getNodeType");
            node.setNodeType((String) getNodeTypeMethod.invoke(nodeDTO));
            
            java.lang.reflect.Method getDescriptionMethod = nodeDTO.getClass().getDeclaredMethod("getDescription");
            node.setDescription((String) getDescriptionMethod.invoke(nodeDTO));
            
            // 设置节点状态，默认为 PENDING
            node.setNodeStatus(NodeStatus.PENDING);
            
        } catch (Exception e) {
            throw new BusinessNotAllowException("转换节点失败：" + e.getMessage());
        }
        
        return node;
    }
}

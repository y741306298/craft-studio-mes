package com.mes.domain.manufacturer.procedureFlow.service;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.mes.domain.manufacturer.procedureFlow.repository.ProcedureFlowRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import com.mes.domain.shared.utils.IdGenerator;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProcedureFlowService {

    @Autowired
    private ProcedureFlowRepository procedureFlowRepository;

    public List<ProcedureFlow> findProcedureFlowsByName(String procedureFlowName, int current, int size) {

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(procedureFlowName)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程名称不能为空");
        }

        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("procedureFlowName", procedureFlowName);
        return procedureFlowRepository.fuzzySearch(searchFilters, current, size);
    }

    public long getTotalCount(String procedureFlowName) {
        if (StringUtils.isNotBlank(procedureFlowName)) {
            Map<String, String> searchFilters = new HashMap<>();
            searchFilters.put("procedureFlowName", procedureFlowName);
            return procedureFlowRepository.totalByFuzzySearch(searchFilters);
        } else {
            return procedureFlowRepository.total();
        }
    }

    public void addProcedureFlow(ProcedureFlow flow) {
        if (flow == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程不能为空");
        }
        if (StringUtils.isBlank(flow.getProcedureFlowName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程名称不能为空");
        }
        
        // 生成唯一的 procedureFlowId
        String procedureFlowId = IdGenerator.generateId("FLOW");
        flow.setProcedureFlowId(procedureFlowId);
        
        procedureFlowRepository.add(flow);
    }

    public void updateProcedureFlow(ProcedureFlow flow) {
        if (flow == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程不能为空");
        }
        if (StringUtils.isBlank(flow.getId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程 ID 不能为空");
        }
        if (StringUtils.isBlank(flow.getProcedureFlowName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程名称不能为空");
        }
        procedureFlowRepository.update(flow);
    }

    public void deleteProcedureFlow(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程 ID 不能为空");
        }
        ProcedureFlow flow = procedureFlowRepository.findById(id);
        if (flow != null) {
            procedureFlowRepository.delete(flow);
        }
    }

    public ProcedureFlow findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程 ID 不能为空");
        }
        return procedureFlowRepository.findById(id);
    }


    public void transferPieceToNextNode(String flowId, String nodeId, Integer quantity) {
        if (StringUtils.isBlank(flowId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程 ID 不能为空");
        }
        if (StringUtils.isBlank(nodeId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "节点 ID 不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "转移的零件数量必须为正整数");
        }

        ProcedureFlow flow = findById(flowId);
        if (flow == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程不存在");
        }

        ProcedureFlowNode currentNode = findNodeById(flow.getNodes(), nodeId);
        if (currentNode == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "当前节点不存在");
        }

        if (currentNode.getPieceQuantity() == null || currentNode.getPieceQuantity() < quantity) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "当前节点零件数量不足");
        }

        currentNode.setPieceQuantity(currentNode.getPieceQuantity() - quantity);

        int currentIndex = getNodeIndex(flow.getNodes(), nodeId);
        if (currentIndex >= 0 && currentIndex < flow.getNodes().size() - 1) {
            ProcedureFlowNode nextNode = flow.getNodes().get(currentIndex + 1);
            if (nextNode.getPieceQuantity() == null) {
                nextNode.setPieceQuantity(0);
            }
            nextNode.setPieceQuantity(nextNode.getPieceQuantity() + quantity);
        } else {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "当前节点已是最后一个节点，无法转移");
        }

        procedureFlowRepository.update(flow);
    }

    public void completeNodeAndTransfer(String flowId, String nodeId) {
        if (StringUtils.isBlank(flowId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程 ID 不能为空");
        }
        if (StringUtils.isBlank(nodeId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "节点 ID 不能为空");
        }

        ProcedureFlow flow = findById(flowId);
        if (flow == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程不存在");
        }

        ProcedureFlowNode currentNode = findNodeById(flow.getNodes(), nodeId);
        if (currentNode == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "当前节点不存在");
        }

        if (currentNode.getNodeStatus() != NodeStatus.ACTIVE) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "只有活动状态的节点才能完成");
        }

        currentNode.updateStatus(NodeStatus.COMPLETED);

        int currentIndex = getNodeIndex(flow.getNodes(), nodeId);
        if (currentIndex >= 0 && currentIndex < flow.getNodes().size() - 1) {
            ProcedureFlowNode nextNode = flow.getNodes().get(currentIndex + 1);
            
            if (currentNode.getPieceQuantity() != null) {
                nextNode.setPieceQuantity(currentNode.getPieceQuantity());
            }
            
            nextNode.updateStatus(NodeStatus.ACTIVE);
        } else {
            flow.setFlowStatus(com.mes.domain.manufacturer.procedureFlow.enums.FlowStatus.COMPLETED);
        }

        procedureFlowRepository.update(flow);
    }

    private ProcedureFlowNode findNodeById(List<ProcedureFlowNode> nodes, String nodeId) {
        if (nodes == null || nodeId == null) {
            return null;
        }
        
        for (ProcedureFlowNode node : nodes) {
            if (nodeId.equals(node.getNodeId())) {
                return node;
            }
        }
        
        return null;
    }

    private int getNodeIndex(List<ProcedureFlowNode> nodes, String nodeId) {
        if (nodes == null || nodeId == null) {
            return -1;
        }
        
        for (int i = 0; i < nodes.size(); i++) {
            if (nodeId.equals(nodes.get(i).getNodeId())) {
                return i;
            }
        }
        
        return -1;
    }

    /**
     * 解析工艺流程，完善生产节点，并将排版和排版中作为第二第三个节点
     * @return 工艺节点列表
     */
    public ProcedureFlow parseProcessingFlow(ProcedureFlow procedureFlow) {
        List<ProcedureFlowNode> defaultNodes = new java.util.ArrayList<>();
        
        ProcedureFlowNode preTreatmentNode = new ProcedureFlowNode();
        preTreatmentNode.setNodeId("NODE_PRETREATMENT");
        preTreatmentNode.setNodeName("预处理");
        preTreatmentNode.setNodeOrder(0);
        preTreatmentNode.setNodeStatus(NodeStatus.PENDING);
        defaultNodes.add(preTreatmentNode);
        
        ProcedureFlowNode typesettingNode = new ProcedureFlowNode();
        typesettingNode.setNodeId("NODE_TYPESETTING");
        typesettingNode.setNodeName("待排版");
        typesettingNode.setNodeOrder(1);
        typesettingNode.setNodeStatus(NodeStatus.PENDING);
        defaultNodes.add(typesettingNode);
        
        ProcedureFlowNode typesettingInProgressNode = new ProcedureFlowNode();
        typesettingInProgressNode.setNodeId("NODE_TYPESETTING_IN_PROGRESS");
        typesettingInProgressNode.setNodeName("排版中");
        typesettingInProgressNode.setNodeOrder(2);
        typesettingInProgressNode.setNodeStatus(NodeStatus.PENDING);
        defaultNodes.add(typesettingInProgressNode);
        
        if (procedureFlow.getNodes() != null && !procedureFlow.getNodes().isEmpty()) {
            for (int i = 0; i < procedureFlow.getNodes().size(); i++) {
                procedureFlow.getNodes().get(i).setNodeOrder(i + 3);
            }
            defaultNodes.addAll(procedureFlow.getNodes());
        }
        
        int lastOrder = defaultNodes.size();
        
        ProcedureFlowNode pendingPackingNode = new ProcedureFlowNode();
        pendingPackingNode.setNodeId("NODE_PENDING_PACKING");
        pendingPackingNode.setNodeName("待打包");
        pendingPackingNode.setNodeOrder(lastOrder);
        pendingPackingNode.setNodeStatus(NodeStatus.PENDING);
        defaultNodes.add(pendingPackingNode);
        
        ProcedureFlowNode packedNode = new ProcedureFlowNode();
        packedNode.setNodeId("NODE_PACKED");
        packedNode.setNodeName("已打包");
        packedNode.setNodeOrder(lastOrder + 1);
        packedNode.setNodeStatus(NodeStatus.PENDING);
        defaultNodes.add(packedNode);
        
        procedureFlow.setNodes(defaultNodes);
        
        return procedureFlow;
    }

    /**
     * 从 MTOProductSpecDTO 中解析工艺流程，转换为 ProcedureFlowNode 列表
     * @param mtoProduct MTO 产品规格 DTO
     * @return 工艺节点列表
     */
    public List<ProcedureFlowNode> parseProcessingFlow(MTOProductSpecDTO mtoProduct) {
        if (mtoProduct == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "MTO 产品规格不能为空");
        }

        Object processFlow = getProcessFlowFromMTOProduct(mtoProduct);
        if (processFlow == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工艺流程不能为空");
        }

        List<Object> nodes = getNodesFromProcessFlow(processFlow);
        if (nodes == null || nodes.isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工艺流程节点不能为空");
        }

        List<ProcedureFlowNode> procedureFlowNodes = new java.util.ArrayList<>();

        ProcedureFlowNode preTreatmentNode = new ProcedureFlowNode();
        preTreatmentNode.setNodeId("NODE_PRETREATMENT");
        preTreatmentNode.setNodeName("预处理");
        preTreatmentNode.setNodeOrder(0);
        preTreatmentNode.setNodeStatus(NodeStatus.PENDING);
        procedureFlowNodes.add(preTreatmentNode);

        ProcedureFlowNode pendingTypesettingNode = new ProcedureFlowNode();
        pendingTypesettingNode.setNodeId("NODE_PENDING_TYPESETTING");
        pendingTypesettingNode.setNodeName("待排版");
        pendingTypesettingNode.setNodeOrder(1);
        pendingTypesettingNode.setNodeStatus(NodeStatus.PENDING);
        procedureFlowNodes.add(pendingTypesettingNode);

        ProcedureFlowNode typesettingNode = new ProcedureFlowNode();
        typesettingNode.setNodeId("NODE_TYPESETTING");
        typesettingNode.setNodeName("排版");
        typesettingNode.setNodeOrder(2);
        typesettingNode.setNodeStatus(NodeStatus.PENDING);
        procedureFlowNodes.add(typesettingNode);


        for (int i = 0; i < nodes.size(); i++) {
            Object nodeObj = nodes.get(i);
            try {
                java.lang.reflect.Method getNodeNameMethod = nodeObj.getClass().getDeclaredMethod("getNodeName");
                String nodeName = (String) getNodeNameMethod.invoke(nodeObj);
                
                ProcedureFlowNode node = new ProcedureFlowNode();
                node.setNodeId("NODE_" + (i + 3));
                node.setNodeName(nodeName != null ? nodeName : "工序" + (i + 1));
                node.setNodeOrder(i + 3);
                node.setNodeStatus(NodeStatus.PENDING);
                procedureFlowNodes.add(node);
            } catch (Exception e) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "解析工艺节点失败：" + e.getMessage());
            }
        }

        int lastOrder = procedureFlowNodes.size() + 2;
        
        ProcedureFlowNode pendingPackingNode = new ProcedureFlowNode();
        pendingPackingNode.setNodeId("NODE_PENDING_PACKING");
        pendingPackingNode.setNodeName("待打包");
        pendingPackingNode.setNodeOrder(lastOrder - 1);
        pendingPackingNode.setNodeStatus(NodeStatus.PENDING);
        procedureFlowNodes.add(pendingPackingNode);

        ProcedureFlowNode packedNode = new ProcedureFlowNode();
        packedNode.setNodeId("NODE_PACKED");
        packedNode.setNodeName("已打包");
        packedNode.setNodeOrder(lastOrder);
        packedNode.setNodeStatus(NodeStatus.PENDING);
        procedureFlowNodes.add(packedNode);

        return procedureFlowNodes;
    }

    /**
     * 从 MTOProductSpecDTO 中获取 processFlow
     */
    private Object getProcessFlowFromMTOProduct(MTOProductSpecDTO mtoProduct) {
        try {
            java.lang.reflect.Method getProcessFlowMethod = mtoProduct.getClass().getDeclaredMethod("getProcessFlow");
            return getProcessFlowMethod.invoke(mtoProduct);
        } catch (Exception e) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "无法获取 processFlow 信息：" + e.getMessage());
        }
    }

    /**
     * 从 processFlow 中获取节点列表
     */
    @SuppressWarnings("unchecked")
    private List<Object> getNodesFromProcessFlow(Object processFlow) {
        try {
            java.lang.reflect.Method getNodesMethod = processFlow.getClass().getDeclaredMethod("getNodes");
            return (List<Object>) getNodesMethod.invoke(processFlow);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查工艺流程中是否包含指定名称的节点
     * @param processingNodes 工艺节点列表
     * @param nodeName 节点名称
     * @return 是否包含
     */
    public boolean hasNodeWithName(List<ProcedureFlowNode> processingNodes, String nodeName) {
        if (processingNodes == null || nodeName == null) {
            return false;
        }
        
        return processingNodes.stream()
                .anyMatch(node -> nodeName.equals(node.getNodeName()));
    }
}

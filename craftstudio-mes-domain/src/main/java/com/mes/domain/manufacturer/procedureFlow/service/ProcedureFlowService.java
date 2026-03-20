package com.mes.domain.manufacturer.procedureFlow.service;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.mes.domain.manufacturer.procedureFlow.repository.ProcedureFlowRepository;
import com.mes.domain.shared.exception.BusinessNotAllowException;
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
            throw new BusinessNotAllowException("每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(procedureFlowName)) {
            throw new BusinessNotAllowException("工序流程名称不能为空");
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
            throw new BusinessNotAllowException("工序流程不能为空");
        }
        if (StringUtils.isBlank(flow.getProcedureFlowName())) {
            throw new BusinessNotAllowException("工序流程名称不能为空");
        }
        procedureFlowRepository.add(flow);
    }

    public void updateProcedureFlow(ProcedureFlow flow) {
        if (flow == null) {
            throw new BusinessNotAllowException("工序流程不能为空");
        }
        if (StringUtils.isBlank(flow.getId())) {
            throw new BusinessNotAllowException("工序流程 ID 不能为空");
        }
        if (StringUtils.isBlank(flow.getProcedureFlowName())) {
            throw new BusinessNotAllowException("工序流程名称不能为空");
        }
        procedureFlowRepository.update(flow);
    }

    public void deleteProcedureFlow(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException("工序流程 ID 不能为空");
        }
        ProcedureFlow flow = procedureFlowRepository.findById(id);
        if (flow != null) {
            procedureFlowRepository.delete(flow);
        }
    }

    public ProcedureFlow findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException("工序流程 ID 不能为空");
        }
        return procedureFlowRepository.findById(id);
    }


    public void transferPieceToNextNode(String flowId, String nodeId, Integer quantity) {
        if (StringUtils.isBlank(flowId)) {
            throw new BusinessNotAllowException("工序流程 ID 不能为空");
        }
        if (StringUtils.isBlank(nodeId)) {
            throw new BusinessNotAllowException("节点 ID 不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessNotAllowException("转移的零件数量必须为正整数");
        }

        ProcedureFlow flow = findById(flowId);
        if (flow == null) {
            throw new BusinessNotAllowException("工序流程不存在");
        }

        ProcedureFlowNode currentNode = findNodeById(flow.getNodes(), nodeId);
        if (currentNode == null) {
            throw new BusinessNotAllowException("当前节点不存在");
        }

        if (currentNode.getPieceQuantity() == null || currentNode.getPieceQuantity() < quantity) {
            throw new BusinessNotAllowException("当前节点零件数量不足");
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
            throw new BusinessNotAllowException("当前节点已是最后一个节点，无法转移");
        }

        procedureFlowRepository.update(flow);
    }

    public void completeNodeAndTransfer(String flowId, String nodeId) {
        if (StringUtils.isBlank(flowId)) {
            throw new BusinessNotAllowException("工序流程 ID 不能为空");
        }
        if (StringUtils.isBlank(nodeId)) {
            throw new BusinessNotAllowException("节点 ID 不能为空");
        }

        ProcedureFlow flow = findById(flowId);
        if (flow == null) {
            throw new BusinessNotAllowException("工序流程不存在");
        }

        ProcedureFlowNode currentNode = findNodeById(flow.getNodes(), nodeId);
        if (currentNode == null) {
            throw new BusinessNotAllowException("当前节点不存在");
        }

        if (currentNode.getNodeStatus() != NodeStatus.ACTIVE) {
            throw new BusinessNotAllowException("只有活动状态的节点才能完成");
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
}

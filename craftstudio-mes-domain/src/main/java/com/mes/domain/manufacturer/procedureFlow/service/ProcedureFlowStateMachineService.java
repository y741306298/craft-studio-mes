package com.mes.domain.manufacturer.procedureFlow.service;

import com.mes.application.dto.resp.ApiResponse;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.FlowStatus;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProcedureFlowStateMachineService {

    /**
     * 添加工序流程
     */
    public ProcedureFlow addProcedureFlow(ProcedureFlow flow) {
        if (flow == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程不能为空");
        }
        if (StringUtils.isBlank(flow.getProcedureFlowName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程名称不能为空");
        }

        // 初始化流程状态
        flow.setFlowStatus(FlowStatus.DRAFT);

        // 设置节点总数
        if (flow.getNodes() != null) {
            flow.setTotalNodes(flow.getNodes().size());

            // 为每个节点设置顺序
            int order = 0;
            for (ProcedureFlowNode node : flow.getNodes()) {
                node.setNodeOrder(order++);
                if (node.getNodeStatus() == null) {
                    node.setNodeStatus(NodeStatus.PENDING);
                }
            }
        } else {
            flow.setTotalNodes(0);
        }

        return flow;
    }

    /**
     * 更新工序流程
     */
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

        // 如果流程已经启动，不允许修改
        if (flow.getFlowStatus() != null &&
            flow.getFlowStatus() != FlowStatus.DRAFT &&
            flow.getFlowStatus() != FlowStatus.NOT_STARTED) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "已启动的流程不允许修改");
        }

        // 更新节点总数和顺序
        if (flow.getNodes() != null) {
            flow.setTotalNodes(flow.getNodes().size());
            int order = 0;
            for (ProcedureFlowNode node : flow.getNodes()) {
                node.setNodeOrder(order++);
            }
        } else {
            flow.setTotalNodes(0);
        }
    }

    /**
     * 删除工序流程
     */
    public void deleteProcedureFlow(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程 ID 不能为空");
        }
        // 实际删除逻辑需要依赖 Repository
    }

    /**
     * 根据ID获取工序流程
     */
    public ProcedureFlow findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程 ID 不能为空");
        }
        // 实际查询逻辑需要依赖 Repository
        return null;
    }

    /**
     * 根据名称查询工序流程（支持分页）
     */
    public List<ProcedureFlow> findProcedureFlowsByName(String procedureFlowName, int current, int size) {

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(procedureFlowName)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程名称不能为空");
        }

        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("procedureFlowName", procedureFlowName);
        // 返回模糊查询结果
        return new ArrayList<>();
    }

    /**
     * 获取工序流程总数
     */
    public long getTotalCount(String procedureFlowName) {
        if (StringUtils.isNotBlank(procedureFlowName)) {
            Map<String, String> searchFilters = new HashMap<>();
            searchFilters.put("procedureFlowName", procedureFlowName);
            return 0L;
        } else {
            return 0L;
        }
    }

    /**
     * 添加工序节点到流程
     */
    public void addNodeToFlow(ProcedureFlow flow, ProcedureFlowNode node) {
        if (flow == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程不能为空");
        }
        if (node == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序节点不能为空");
        }

        // 检查流程是否已启动
        if (flow.getFlowStatus() != null &&
            flow.getFlowStatus() != FlowStatus.DRAFT &&
            flow.getFlowStatus() != FlowStatus.NOT_STARTED) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "已启动的流程不允许添加节点");
        }

        if (flow.getNodes() == null) {
            flow.setNodes(new ArrayList<>());
        }

        node.setNodeOrder(flow.getNodes().size());
        if (node.getNodeStatus() == null) {
            node.setNodeStatus(NodeStatus.PENDING);
        }

        flow.getNodes().add(node);
        flow.setTotalNodes(flow.getNodes().size());
    }

    /**
     * 从流程中移除节点
     */
    public void removeNodeFromFlow(ProcedureFlow flow, String nodeId) {
        if (flow == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序流程不能为空");
        }
        if (StringUtils.isBlank(nodeId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "节点 ID 不能为空");
        }

        // 检查流程是否已启动
        if (flow.getFlowStatus() != null &&
            flow.getFlowStatus() != FlowStatus.DRAFT &&
            flow.getFlowStatus() != FlowStatus.NOT_STARTED) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "已启动的流程不允许移除节点");
        }

        if (flow.getNodes() != null) {
            flow.getNodes().removeIf(node -> nodeId.equals(node.getNodeId()));

            // 重新排序节点
            int order = 0;
            for (ProcedureFlowNode node : flow.getNodes()) {
                node.setNodeOrder(order++);
            }

            flow.setTotalNodes(flow.getNodes().size());
        }
    }

    public void startFlow(ProcedureFlow flow) {
        if (flow == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "流程不能为空");
        }

        if (flow.getFlowStatus() != null && flow.getFlowStatus() != FlowStatus.NOT_STARTED && flow.getFlowStatus() != FlowStatus.DRAFT) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "流程已经启动，无法重复启动");
        }

        if (flow.getNodes() == null || flow.getNodes().isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "流程至少需要一个节点");
        }

        // 初始化所有节点为待处理状态
        List<ProcedureFlowNode> nodes = flow.getNodes();
        if (nodes != null && !nodes.isEmpty()) {
            for (ProcedureFlowNode node : nodes) {
                if (node.getNodeStatus() == null) {
                    node.updateStatus(NodeStatus.PENDING);
                }
            }

            // 设置第一个节点为活动状态
            ProcedureFlowNode firstNode = nodes.get(0);
            firstNode.updateStatus(NodeStatus.ACTIVE);
        }

        flow.setFlowStatus(FlowStatus.RUNNING);
    }

    public ProcedureFlowNode moveToNextNode(ProcedureFlow flow, String currentNodeId, String operatorId, String operatorName) {
        // 完成当前节点
        completeNode(flow, currentNodeId, operatorId, operatorName);

        // 找到下一个节点
        List<ProcedureFlowNode> nodes = flow.getNodes();
        ProcedureFlowNode currentNode = findNodeById(nodes, currentNodeId);

        if (currentNode == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "当前节点不存在");
        }

        Integer currentIndex = currentNode.getNodeOrder();
        if (currentIndex == null || currentIndex >= nodes.size() - 1) {
            // 已经是最后一个节点，完成流程
            flow.setFlowStatus(FlowStatus.COMPLETED);
            return null;
        }

        // 激活下一个节点
        ProcedureFlowNode nextNode = nodes.get(currentIndex + 1);
        nextNode.updateStatus(NodeStatus.ACTIVE);
        nextNode.setOperatorId(operatorId);
        nextNode.setOperatorName(operatorName);

        return nextNode;
    }

    public void completeNode(ProcedureFlow flow, String nodeId, String operatorId, String operatorName) {
        ProcedureFlowNode node = findNodeById(flow.getNodes(), nodeId);
        if (node == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "节点不存在");
        }

        if (node.getNodeStatus() != NodeStatus.ACTIVE) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "只有活动状态的节点才能完成");
        }

        node.updateStatus(NodeStatus.COMPLETED);
        node.setOperatorId(operatorId);
        node.setOperatorName(operatorName);
    }

    public void failNode(ProcedureFlow flow, String nodeId, String errorMessage) {
        ProcedureFlowNode node = findNodeById(flow.getNodes(), nodeId);
        if (node == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "节点不存在");
        }

        node.updateStatus(NodeStatus.FAILED);
        node.setErrorMessage(errorMessage);

        // 更新流程状态为失败
        flow.setFlowStatus(FlowStatus.FAILED);
    }

    public void skipNode(ProcedureFlow flow, String nodeId, String operatorId, String operatorName) {
        ProcedureFlowNode node = findNodeById(flow.getNodes(), nodeId);
        if (node == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "节点不存在");
        }

        if (node.getNodeStatus() != NodeStatus.PENDING && node.getNodeStatus() != NodeStatus.ACTIVE) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "只能跳过待处理或活动状态的节点");
        }

        node.updateStatus(NodeStatus.SKIPPED);
        node.setOperatorId(operatorId);
        node.setOperatorName(operatorName);
    }

    public void suspendFlow(ProcedureFlow flow) {
        if (flow.getFlowStatus() != FlowStatus.RUNNING) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "只有运行中的流程才能暂停");
        }

        flow.setFlowStatus(FlowStatus.SUSPENDED);
    }

    public void resumeFlow(ProcedureFlow flow) {
        if (flow.getFlowStatus() != FlowStatus.SUSPENDED) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "只有暂停的流程才能恢复");
        }

        flow.setFlowStatus(FlowStatus.RUNNING);
    }

    public void cancelFlow(ProcedureFlow flow) {
        if (flow.getFlowStatus() == null || flow.getFlowStatus().isTerminal()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "终态流程无法取消");
        }

        flow.setFlowStatus(FlowStatus.CANCELLED);

        // 取消所有待处理和活动节点
        List<ProcedureFlowNode> nodes = flow.getNodes();
        if (nodes != null) {
            for (ProcedureFlowNode node : nodes) {
                if (node.getNodeStatus() == NodeStatus.PENDING || node.getNodeStatus() == NodeStatus.ACTIVE) {
                    node.updateStatus(NodeStatus.CANCELLED);
                }
            }
        }
    }

    public void retryNode(ProcedureFlow flow, String nodeId) {
        ProcedureFlowNode node = findNodeById(flow.getNodes(), nodeId);
        if (node == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "节点不存在");
        }

        if (node.getNodeStatus() != NodeStatus.FAILED) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "只能重试失败状态的节点");
        }

        node.setRetryCount(node.getRetryCount() != null ? node.getRetryCount() + 1 : 1);
        node.setErrorMessage(null);
        node.updateStatus(NodeStatus.ACTIVE);

        // 恢复流程状态
        if (flow.getFlowStatus() == FlowStatus.FAILED) {
            flow.setFlowStatus(FlowStatus.RUNNING);
        }
    }

    public NodeStatus getNodeStatus(ProcedureFlow flow, String nodeId) {
        ProcedureFlowNode node = findNodeById(flow.getNodes(), nodeId);
        return node != null ? node.getNodeStatus() : null;
    }

    public FlowStatus getFlowStatus(ProcedureFlow flow) {
        return flow.getFlowStatus();
    }

    /**
     * 根据ID 查找节点
     */
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
}

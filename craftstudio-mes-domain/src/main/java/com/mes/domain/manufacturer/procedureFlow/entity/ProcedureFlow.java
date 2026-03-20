package com.mes.domain.manufacturer.procedureFlow.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.procedureFlow.enums.FlowStatus;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProcedureFlow extends BaseEntity {

    private String procedureFlowId;
    private String procedureFlowName;
    private String flowDescription;
    private FlowStatus flowStatus; // 流程整体状态
    private List<ProcedureFlowNode> nodes; // 流程节点列表
    private Integer totalNodes; // 节点总数

    /**
     * 获取节点总数
     */
    public int getNodeCount() {
        return nodes != null ? nodes.size() : 0;
    }

    /**
     * 获取指定索引的节点
     */
    public ProcedureFlowNode getNode(int index) {
        if (nodes != null && index >= 0 && index < nodes.size()) {
            return nodes.get(index);
        }
        return null;
    }

    /**
     * 获取当前活动节点
     */
    public ProcedureFlowNode getCurrentActiveNode() {
        if (nodes != null) {
            for (ProcedureFlowNode node : nodes) {
                if (NodeStatus.ACTIVE == node.getNodeStatus()) {
                    return node;
                }
            }
        }
        return null;
    }
}
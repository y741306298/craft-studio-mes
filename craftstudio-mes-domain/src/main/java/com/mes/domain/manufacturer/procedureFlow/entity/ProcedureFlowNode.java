package com.mes.domain.manufacturer.procedureFlow.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProcedureFlowNode extends BaseEntity {

    private String nodeId;
    private String nodeName;
    private String procedureId;
    private Integer nodeOrder;
    private NodeStatus nodeStatus;
    private String nodeType;
    private String description;
    private String operatorId;
    private String operatorName;
    private Date startTime;
    private Date endTime;
    private String remarks;
    private Integer retryCount;
    private String errorMessage;
    private Integer pieceQuantity;

    public boolean isStartNode() {
        return "START".equals(this.nodeType);
    }
    
    public boolean isEndNode() {
        return "END".equals(this.nodeType);
    }
    
    public void updateStatus(NodeStatus newStatus) {
        this.nodeStatus = newStatus;
        if (NodeStatus.ACTIVE == newStatus) {
            this.startTime = new Date();
        } else if (NodeStatus.COMPLETED == newStatus || NodeStatus.FAILED == newStatus) {
            this.endTime = new Date();
        }
    }
}

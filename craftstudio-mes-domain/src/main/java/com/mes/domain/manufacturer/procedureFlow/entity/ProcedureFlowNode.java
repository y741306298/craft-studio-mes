package com.mes.domain.manufacturer.procedureFlow.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.Process;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

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
    private Process.ProcessMetaSnapshot processMetaSnapshot;
    private List<MTOProductSpecDTO.ProcessParamConfigDTO> paramConfigs;

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

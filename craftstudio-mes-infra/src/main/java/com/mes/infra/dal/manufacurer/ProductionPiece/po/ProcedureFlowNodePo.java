package com.mes.infra.dal.manufacurer.ProductionPiece.po;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.mes.infra.base.BasePO;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProcedureFlowNodePo extends BasePO<ProcedureFlowNode> {

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
    private List<MTOProductSpecDTO.ProcessParamConfigDTO> paramConfigs;

    @Override
    public ProcedureFlowNode toDO() {
        ProcedureFlowNode node = new ProcedureFlowNode();
        node.setId(getId());
        node.setCreateTime(getCreateTime());
        node.setUpdateTime(getUpdateTime());

        node.setNodeId(this.nodeId);
        node.setNodeName(this.nodeName);
        node.setProcedureId(this.procedureId);
        node.setNodeOrder(this.nodeOrder);
        node.setNodeStatus(this.nodeStatus);
        node.setNodeType(this.nodeType);
        node.setDescription(this.description);
        node.setOperatorId(this.operatorId);
        node.setOperatorName(this.operatorName);
        node.setStartTime(this.startTime);
        node.setEndTime(this.endTime);
        node.setRemarks(this.remarks);
        node.setRetryCount(this.retryCount);
        node.setErrorMessage(this.errorMessage);
        node.setPieceQuantity(this.pieceQuantity);
        node.setParamConfigs(this.paramConfigs);

        return node;
    }

    @Override
    protected BasePO<ProcedureFlowNode> fromDO(ProcedureFlowNode _do) {
        if (_do == null) {
            return null;
        }

        this.nodeId = _do.getNodeId();
        this.nodeName = _do.getNodeName();
        this.procedureId = _do.getProcedureId();
        this.nodeOrder = _do.getNodeOrder();
        this.nodeStatus = _do.getNodeStatus();
        this.nodeType = _do.getNodeType();
        this.description = _do.getDescription();
        this.operatorId = _do.getOperatorId();
        this.operatorName = _do.getOperatorName();
        this.startTime = _do.getStartTime();
        this.endTime = _do.getEndTime();
        this.remarks = _do.getRemarks();
        this.retryCount = _do.getRetryCount();
        this.errorMessage = _do.getErrorMessage();
        this.pieceQuantity = _do.getPieceQuantity();
        this.paramConfigs = _do.getParamConfigs();

        return this;
    }
}

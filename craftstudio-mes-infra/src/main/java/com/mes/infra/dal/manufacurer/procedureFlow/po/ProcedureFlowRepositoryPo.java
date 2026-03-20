package com.mes.infra.dal.manufacurer.procedureFlow.po;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.FlowStatus;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "procedureFlow")
public class ProcedureFlowRepositoryPo extends BasePO<ProcedureFlow> {

    private String procedureFlowId;
    private String procedureFlowName;
    private String flowDescription;
    private FlowStatus flowStatus;
    private List<ProcedureFlowNode> nodes;
    private Integer totalNodes;

    @Override
    public ProcedureFlow toDO() {
        ProcedureFlow procedureFlow = new ProcedureFlow();
        copyBaseFieldsToDO(procedureFlow);
        procedureFlow.setProcedureFlowId(this.procedureFlowId);
        procedureFlow.setProcedureFlowName(this.procedureFlowName);
        procedureFlow.setFlowDescription(this.flowDescription);
        procedureFlow.setFlowStatus(this.flowStatus);
        procedureFlow.setNodes(this.nodes);
        procedureFlow.setTotalNodes(this.totalNodes);
        return procedureFlow;
    }

    @Override
    protected BasePO<ProcedureFlow> fromDO(ProcedureFlow _do) {
        if (_do == null) {
            return null;
        }
        
        this.procedureFlowId = _do.getProcedureFlowId();
        this.procedureFlowName = _do.getProcedureFlowName();
        this.flowDescription = _do.getFlowDescription();
        this.flowStatus = _do.getFlowStatus();
        this.nodes = _do.getNodes();
        this.totalNodes = _do.getTotalNodes();
        
        return this;
    }
}

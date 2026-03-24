package com.mes.interfaces.api.dto.req.procedureFlow;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.FlowStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ProcedureFlowRequest {

    private String id;

    private String procedureFlowId;

    @NotBlank(message = "工序流程名称不能为空")
    private String procedureFlowName;

    private String flowDescription;

    private FlowStatus flowStatus;

    private List<ProcedureFlowNode> nodes;

    private Integer totalNodes;

    public ProcedureFlow toDomainEntity() {
        ProcedureFlow flow = new ProcedureFlow();
        flow.setId(this.id);
        flow.setProcedureFlowId(this.procedureFlowId);
        flow.setProcedureFlowName(this.procedureFlowName);
        flow.setFlowDescription(this.flowDescription);
        flow.setFlowStatus(this.flowStatus);
        flow.setNodes(this.nodes);
        flow.setTotalNodes(this.totalNodes);
        return flow;
    }
}

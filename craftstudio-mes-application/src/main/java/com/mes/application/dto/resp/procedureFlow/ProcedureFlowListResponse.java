package com.mes.application.dto.resp.procedureFlow;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.FlowStatus;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ProcedureFlowListResponse {

    private String id;
    private String procedureFlowId;
    private String procedureFlowName;
    private String flowDescription;
    private FlowStatus flowStatus;
    private List<ProcedureFlowNode> nodes;
    private Integer totalNodes;
    private Date createTime;
    private Date updateTime;

    public static ProcedureFlowListResponse from(ProcedureFlow flow) {
        if (flow == null) {
            return null;
        }

        ProcedureFlowListResponse response = new ProcedureFlowListResponse();
        response.setId(flow.getId());
        response.setProcedureFlowId(flow.getProcedureFlowId());
        response.setProcedureFlowName(flow.getProcedureFlowName());
        response.setFlowDescription(flow.getFlowDescription());
        response.setFlowStatus(flow.getFlowStatus());
        response.setNodes(flow.getNodes());
        response.setTotalNodes(flow.getTotalNodes());
        response.setCreateTime(flow.getCreateTime());
        response.setUpdateTime(flow.getUpdateTime());

        return response;
    }
}

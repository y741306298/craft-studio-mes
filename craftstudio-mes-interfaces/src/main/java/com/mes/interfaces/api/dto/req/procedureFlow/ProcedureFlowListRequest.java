package com.mes.interfaces.api.dto.req.procedureFlow;

import com.mes.interfaces.api.dto.req.base.PagedApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProcedureFlowListRequest extends PagedApiRequest {
    private String procedureFlowName;
}

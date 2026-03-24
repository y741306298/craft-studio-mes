package com.mes.interfaces.api.dto.req.procedure;

import com.mes.interfaces.api.dto.req.base.PagedApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProcedureListRequest extends PagedApiRequest {
    private String procedureName;
}

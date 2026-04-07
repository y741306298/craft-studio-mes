package com.mes.application.command.api.req;

import com.mes.domain.base.UnitPrice;
import lombok.Data;

@Data
public class ConfigProcessMetaRequest {

    private String rmfId;
    private String processMetaId;
    private UnitPrice unitPrice;

}

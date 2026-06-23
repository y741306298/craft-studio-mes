package com.mes.application.dto.req.manufacturerMaterialLayoutSpecCfg;

import com.mes.application.dto.req.base.PagedApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ManufacturerMaterialLayoutSpecCfgListRequest extends PagedApiRequest {
    private String manufacturerMetaId;
    private String materialLayoutSpecId;
}

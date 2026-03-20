package com.mes.domain.manufacturer.manufacturerMtsProductCfg.vo;

import com.mes.domain.base.UnitPrice;
import com.mes.domain.manufacturer.enums.CfgStatus;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import lombok.Data;

import java.util.List;

@Data
public class ManufacturerMtsProductSpec {

    private String id;
    private String name;
    private String previewUrl;
    private List<String> materials;
    private List<ProcedureFlow> procedureFlow;
    private Boolean customizable;
    private UnitPrice price;
    private CfgStatus status;

}

package com.mes.domain.manufacturer.manufacturerProcessPriceCfg.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.base.UnitPrice;
import com.mes.domain.manufacturer.enums.CfgStatus;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.vo.MaterialProcessPrice;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ManufacturerProcessPriceCfg extends BaseEntity {
    private String processId;
    private String manufacturerId;
    private UnitPrice processPrice;
    private Double basePrice;
    private String processName;
    private String processCode;
    private String processDescription;
    private String processType;
    private String capacity;
    private String unit;
    private ProcedureFlow procedureFlow;
    private String materials;
    private CfgStatus status;
    private List<MaterialProcessPrice> materialProcessPrices;
}

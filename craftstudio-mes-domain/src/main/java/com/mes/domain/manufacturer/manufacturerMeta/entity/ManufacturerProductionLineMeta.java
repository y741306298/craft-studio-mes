package com.mes.domain.manufacturer.manufacturerMeta.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ManufacturerProductionLineMeta extends BaseEntity {

    private String productionLineId;
    private String productionLineName;
    private List<ManufacturerProcedureMeta> manufacturerProceduresMetas;

}

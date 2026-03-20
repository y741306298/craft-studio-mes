package com.mes.domain.manufacturer.productionPiece.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProductionCell extends BaseEntity {

    private String cellId;
    private String pieceId;
    private String nodeId;
    private ProcedureFlow procedureFlow;

}

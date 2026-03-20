package com.mes.domain.manufacturer.productionPiece.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProductionPiece extends BaseEntity {

    private String productionPieceId;
    private String orderItemId;
    private String procedureFlowId;
    private String status;
    private String productionPieceType;
    private Integer quantity;
    private String templateCode;
    private String positionType;
    private String positionCode;
    private ProcedureFlow procedureFlow;

}

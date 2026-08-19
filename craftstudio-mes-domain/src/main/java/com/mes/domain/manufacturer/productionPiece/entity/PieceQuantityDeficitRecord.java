package com.mes.domain.manufacturer.productionPiece.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PieceQuantityDeficitRecord extends BaseEntity {
    private String productionPieceId;
    private String productionPieceRecordId;
    private String fromNodeId;
    private String fromNodeName;
    private String toNodeId;
    private String toNodeName;
    private Integer requestedQuantity;
    private Integer sourceQuantity;
    private Integer deficitQuantity;
}

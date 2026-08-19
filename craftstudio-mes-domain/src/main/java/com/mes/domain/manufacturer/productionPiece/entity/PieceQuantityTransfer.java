package com.mes.domain.manufacturer.productionPiece.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PieceQuantityTransfer {
    private String productionPieceId;
    private String fromNodeId;
    private String toNodeId;
    private Integer quantity;
}

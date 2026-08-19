package com.mes.infra.dal.manufacurer.ProductionPiece.po;

import com.mes.domain.manufacturer.productionPiece.entity.PieceQuantityDeficitRecord;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "pieceQuantityDeficitRecord")
public class PieceQuantityDeficitRecordPo extends BasePO<PieceQuantityDeficitRecord> {
    private String productionPieceId;
    private String productionPieceRecordId;
    private String fromNodeId;
    private String fromNodeName;
    private String toNodeId;
    private String toNodeName;
    private Integer requestedQuantity;
    private Integer sourceQuantity;
    private Integer deficitQuantity;

    @Override
    public PieceQuantityDeficitRecord toDO() {
        PieceQuantityDeficitRecord record = new PieceQuantityDeficitRecord();
        record.setId(getId());
        record.setCreateTime(getCreateTime());
        record.setUpdateTime(getUpdateTime());
        record.setProductionPieceId(productionPieceId);
        record.setProductionPieceRecordId(productionPieceRecordId);
        record.setFromNodeId(fromNodeId);
        record.setFromNodeName(fromNodeName);
        record.setToNodeId(toNodeId);
        record.setToNodeName(toNodeName);
        record.setRequestedQuantity(requestedQuantity);
        record.setSourceQuantity(sourceQuantity);
        record.setDeficitQuantity(deficitQuantity);
        return record;
    }

    @Override
    protected BasePO<PieceQuantityDeficitRecord> fromDO(PieceQuantityDeficitRecord record) {
        productionPieceId = record.getProductionPieceId();
        productionPieceRecordId = record.getProductionPieceRecordId();
        fromNodeId = record.getFromNodeId();
        fromNodeName = record.getFromNodeName();
        toNodeId = record.getToNodeId();
        toNodeName = record.getToNodeName();
        requestedQuantity = record.getRequestedQuantity();
        sourceQuantity = record.getSourceQuantity();
        deficitQuantity = record.getDeficitQuantity();
        return this;
    }
}

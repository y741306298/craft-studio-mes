package com.mes.infra.dal.manufacurer.transBox.storageOperationRecord.po;

import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageOperationRecord;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "storageOperationRecord")
public class StorageOperationRecordPo extends BasePO<StorageOperationRecord> {

    private String recordId;
    private String storageTankId;
    private String storageTankName;
    private String slotId;
    private String operationType;
    private String productionPieceId;
    private String productionPieceType;
    private Integer quantity;
    private String operatorId;
    private String operatorName;
    private Date operationTime;
    private String remarks;
    private String status;

    @Override
    public StorageOperationRecord toDO() {
        StorageOperationRecord record = new StorageOperationRecord();
        record.setId(getId());
        record.setCreateTime(getCreateTime());
        record.setUpdateTime(getUpdateTime());

        record.setRecordId(this.recordId);
        record.setStorageTankId(this.storageTankId);
        record.setStorageTankName(this.storageTankName);
        record.setSlotId(this.slotId);
        record.setOperationType(this.operationType);
        record.setProductionPieceId(this.productionPieceId);
        record.setProductionPieceType(this.productionPieceType);
        record.setQuantity(this.quantity);
        record.setOperatorId(this.operatorId);
        record.setOperatorName(this.operatorName);
        record.setOperationTime(this.operationTime);
        record.setRemarks(this.remarks);
        record.setStatus(this.status);

        return record;
    }

    @Override
    protected BasePO<StorageOperationRecord> fromDO(StorageOperationRecord _do) {
        if (_do == null) {
            return null;
        }

        this.recordId = _do.getRecordId();
        this.storageTankId = _do.getStorageTankId();
        this.storageTankName = _do.getStorageTankName();
        this.slotId = _do.getSlotId();
        this.operationType = _do.getOperationType();
        this.productionPieceId = _do.getProductionPieceId();
        this.productionPieceType = _do.getProductionPieceType();
        this.quantity = _do.getQuantity();
        this.operatorId = _do.getOperatorId();
        this.operatorName = _do.getOperatorName();
        this.operationTime = _do.getOperationTime();
        this.remarks = _do.getRemarks();
        this.status = _do.getStatus();

        return this;
    }
}

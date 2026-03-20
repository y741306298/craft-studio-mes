package com.mes.infra.dal.manufacurer.transBox.storageSlot.po;

import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageSlot;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "storageSlot")
public class StorageSlotPo extends BasePO<StorageSlot> {
    
    private String slotId;
    private String slotCode;
    private String storageTankId;
    private Integer slotOrder;
    private String status;
    private String productionPieceId;
    private String productionPieceType;
    private Integer quantity;
    private Date storageTime;
    private Date outTime;
    private String remarks;
    private Double weight;
    private Double volume;
    private String temperature;
    private String humidity;

    @Override
    public StorageSlot toDO() {
        StorageSlot storageSlot = new StorageSlot();
        storageSlot.setId(getId());
        storageSlot.setCreateTime(getCreateTime());
        storageSlot.setUpdateTime(getUpdateTime());
        
        storageSlot.setSlotId(this.slotId);
        storageSlot.setSlotCode(this.slotCode);
        storageSlot.setStorageTankId(this.storageTankId);
        storageSlot.setSlotOrder(this.slotOrder);
        storageSlot.setStatus(this.status);
        storageSlot.setProductionPieceId(this.productionPieceId);
        storageSlot.setProductionPieceType(this.productionPieceType);
        storageSlot.setQuantity(this.quantity);
        storageSlot.setStorageTime(this.storageTime);
        storageSlot.setOutTime(this.outTime);
        storageSlot.setRemarks(this.remarks);
        storageSlot.setWeight(this.weight);
        storageSlot.setVolume(this.volume);
        storageSlot.setTemperature(this.temperature);
        storageSlot.setHumidity(this.humidity);
        
        return storageSlot;
    }

    @Override
    protected BasePO<StorageSlot> fromDO(StorageSlot _do) {
        if (_do == null) {
            return null;
        }
        
        this.slotId = _do.getSlotId();
        this.slotCode = _do.getSlotCode();
        this.storageTankId = _do.getStorageTankId();
        this.slotOrder = _do.getSlotOrder();
        this.status = _do.getStatus();
        this.productionPieceId = _do.getProductionPieceId();
        this.productionPieceType = _do.getProductionPieceType();
        this.quantity = _do.getQuantity();
        this.storageTime = _do.getStorageTime();
        this.outTime = _do.getOutTime();
        this.remarks = _do.getRemarks();
        this.weight = _do.getWeight();
        this.volume = _do.getVolume();
        this.temperature = _do.getTemperature();
        this.humidity = _do.getHumidity();
        
        return this;
    }
}

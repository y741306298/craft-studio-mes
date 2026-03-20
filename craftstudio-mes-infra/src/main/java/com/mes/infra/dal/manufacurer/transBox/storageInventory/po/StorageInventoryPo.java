package com.mes.infra.dal.manufacurer.transBox.storageInventory.po;

import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageInventory;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "storageInventory")
public class StorageInventoryPo extends BasePO<StorageInventory> {
    
    private String inventoryId;
    private String storageTankId;
    private String storageTankName;
    private String productionPieceId;
    private String productionPieceType;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private Integer lockedQuantity;
    private String unit;
    private Date lastCheckTime;
    private String lastCheckOperator;
    private String status;

    @Override
    public StorageInventory toDO() {
        StorageInventory storageInventory = new StorageInventory();
        storageInventory.setId(getId());
        storageInventory.setCreateTime(getCreateTime());
        storageInventory.setUpdateTime(getUpdateTime());
        
        storageInventory.setInventoryId(this.inventoryId);
        storageInventory.setStorageTankId(this.storageTankId);
        storageInventory.setStorageTankName(this.storageTankName);
        storageInventory.setProductionPieceId(this.productionPieceId);
        storageInventory.setProductionPieceType(this.productionPieceType);
        storageInventory.setTotalQuantity(this.totalQuantity);
        storageInventory.setAvailableQuantity(this.availableQuantity);
        storageInventory.setLockedQuantity(this.lockedQuantity);
        storageInventory.setUnit(this.unit);
        storageInventory.setLastCheckTime(this.lastCheckTime);
        storageInventory.setLastCheckOperator(this.lastCheckOperator);
        storageInventory.setStatus(this.status);
        
        return storageInventory;
    }

    @Override
    protected BasePO<StorageInventory> fromDO(StorageInventory _do) {
        if (_do == null) {
            return null;
        }
        
        this.inventoryId = _do.getInventoryId();
        this.storageTankId = _do.getStorageTankId();
        this.storageTankName = _do.getStorageTankName();
        this.productionPieceId = _do.getProductionPieceId();
        this.productionPieceType = _do.getProductionPieceType();
        this.totalQuantity = _do.getTotalQuantity();
        this.availableQuantity = _do.getAvailableQuantity();
        this.lockedQuantity = _do.getLockedQuantity();
        this.unit = _do.getUnit();
        this.lastCheckTime = _do.getLastCheckTime();
        this.lastCheckOperator = _do.getLastCheckOperator();
        this.status = _do.getStatus();
        
        return this;
    }
}

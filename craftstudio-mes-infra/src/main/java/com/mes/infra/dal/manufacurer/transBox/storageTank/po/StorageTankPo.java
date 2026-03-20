package com.mes.infra.dal.manufacurer.transBox.storageTank.po;

import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageSlot;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageTank;
import com.mes.infra.base.BasePO;
import com.mes.infra.dal.manufacurer.transBox.storageSlot.po.StorageSlotPo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "storageTank")
public class StorageTankPo extends BasePO<StorageTank> {
    
    private String storageTankId;
    private String storageTankName;
    private String storageTankCode;
    private String storageTankType;
    private String location;
    private String status;
    private Boolean movable;
    private Integer totalSlots;
    private Integer usedSlots;
    private Integer remainingSlots;
    private Double maxCapacity;
    private Double currentCapacity;
    private String capacityUnit;
    private String temperatureRange;
    private String humidityRange;
    private String description;
    private List<StorageSlotPo> storageSlots;
    private String managerId;
    private String managerName;

    @Override
    public StorageTank toDO() {
        StorageTank storageTank = new StorageTank();
        storageTank.setId(getId());
        storageTank.setCreateTime(getCreateTime());
        storageTank.setUpdateTime(getUpdateTime());
        
        storageTank.setStorageTankId(this.storageTankId);
        storageTank.setStorageTankName(this.storageTankName);
        storageTank.setStorageTankCode(this.storageTankCode);
        storageTank.setStorageTankType(this.storageTankType);
        storageTank.setLocation(this.location);
        storageTank.setStatus(this.status);
        storageTank.setMovable(this.movable);
        storageTank.setTotalSlots(this.totalSlots);
        storageTank.setUsedSlots(this.usedSlots);
        storageTank.setRemainingSlots(this.remainingSlots);
        storageTank.setMaxCapacity(this.maxCapacity);
        storageTank.setCurrentCapacity(this.currentCapacity);
        storageTank.setCapacityUnit(this.capacityUnit);
        storageTank.setTemperatureRange(this.temperatureRange);
        storageTank.setHumidityRange(this.humidityRange);
        storageTank.setDescription(this.description);
        storageTank.setManagerId(this.managerId);
        storageTank.setManagerName(this.managerName);
        
        if (this.storageSlots != null && !this.storageSlots.isEmpty()) {
            List<StorageSlot> slotList = this.storageSlots.stream()
                .map(StorageSlotPo::toDO)
                .toList();
            storageTank.setStorageSlots(slotList);
        }
        
        return storageTank;
    }

    @Override
    protected BasePO<StorageTank> fromDO(StorageTank _do) {
        if (_do == null) {
            return null;
        }
        
        this.storageTankId = _do.getStorageTankId();
        this.storageTankName = _do.getStorageTankName();
        this.storageTankCode = _do.getStorageTankCode();
        this.storageTankType = _do.getStorageTankType();
        this.location = _do.getLocation();
        this.status = _do.getStatus();
        this.movable = _do.getMovable();
        this.totalSlots = _do.getTotalSlots();
        this.usedSlots = _do.getUsedSlots();
        this.remainingSlots = _do.getRemainingSlots();
        this.maxCapacity = _do.getMaxCapacity();
        this.currentCapacity = _do.getCurrentCapacity();
        this.capacityUnit = _do.getCapacityUnit();
        this.temperatureRange = _do.getTemperatureRange();
        this.humidityRange = _do.getHumidityRange();
        this.description = _do.getDescription();
        this.managerId = _do.getManagerId();
        this.managerName = _do.getManagerName();
        
        if (_do.getStorageSlots() != null && !_do.getStorageSlots().isEmpty()) {
            List<StorageSlotPo> slotPos = _do.getStorageSlots().stream()
                .map(slot -> StorageSlotPo.fromDO(slot, StorageSlotPo.class))
                .toList();
            this.storageSlots = slotPos;
        }
        
        return this;
    }
}

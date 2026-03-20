package com.mes.application.command.manufacturerMeta;

import com.mes.domain.manufacturer.device.repository.DeviceInfoRepository;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerMeta;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerMetaRepository;
import com.mes.domain.manufacturer.manufacturerMeta.service.ManufacturerMetaService;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageSlot;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageTank;
import com.mes.domain.manufacturer.transBox.storageTank.repository.StorageTankRepository;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class AppManufacturerMetaService {

    @Autowired
    private ManufacturerMetaService domainManufacturerMetaService;

    @Autowired
    private ManufacturerMetaRepository manufacturerMetaRepository;
    
    @Autowired
    private DeviceInfoRepository deviceInfoRepository;

    @Autowired
    private StorageTankRepository storageTankRepository;

    public PagedResult<ManufacturerMeta> findManufacturerMetas(String name, String manufacturerType, PagedQuery query){
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (query.getSize() <= 0 || query.getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在 1-100 之间");
        }

        List<ManufacturerMeta> items;
        long total;

        if (StringUtils.isBlank(name) && StringUtils.isBlank(manufacturerType)) {
            items = manufacturerMetaRepository.list(query.getCurrent(), query.getSize());
            total = manufacturerMetaRepository.total();
        } else {
            items = domainManufacturerMetaService.findManufacturerMetasByConditions(name, manufacturerType, (int) query.getCurrent(), query.getSize());
            total = domainManufacturerMetaService.getTotalCount(name, manufacturerType);
        }

        return new PagedResult<ManufacturerMeta>(items, total, query.getSize(), query.getCurrent());
    }

    public void addManufacturerMeta(ManufacturerMeta command){
        if (command == null) {
            throw new IllegalArgumentException("制造商元数据不能为空");
        }
        
        domainManufacturerMetaService.addManufacturerMeta(command);
        
        createDefaultStorageTank(command);
    }
    
    private void createDefaultStorageTank(ManufacturerMeta manufacturerMeta) {
        String manufacturerId = manufacturerMeta.getId();
        if (StringUtils.isBlank(manufacturerId)) {
            return;
        }
        
        StorageTank storageTank = new StorageTank();
        storageTank.setManufacturerId(manufacturerId);
        storageTank.setStorageTankName("默认储存柜");
        storageTank.setStorageTankCode("STORAGE_DEFAULT_" + System.currentTimeMillis());
        storageTank.setStorageTankType("DEFAULT");
        storageTank.setLocation("默认位置");
        storageTank.setStatus("ACTIVE");
        storageTank.setMovable(false);
        
        List<StorageSlot> storageSlots = new ArrayList<>();
        int slotIndex = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                StorageSlot slot = new StorageSlot();
                slot.setSlotId(UUID.randomUUID().toString());
                slot.setSlotCode("SLOT_" + row + "_" + col);
                slot.setStorageTankId(storageTank.getStorageTankId());
                slot.setSlotOrder(slotIndex++);
                slot.setStatus("AVAILABLE");
                storageSlots.add(slot);
            }
        }
        
        storageTank.setStorageSlots(storageSlots);
        storageTank.setTotalSlots(9);
        storageTank.setUsedSlots(0);
        storageTank.setRemainingSlots(9);
        storageTank.setMaxCapacity(1000.0);
        storageTank.setCurrentCapacity(0.0);
        storageTank.setCapacityUnit("kg");
        storageTank.setDescription("系统自动创建的默认 3*3 储存柜");
        
        storageTankRepository.add(storageTank);
    }
    
    public void updateManufacturerMeta(ManufacturerMeta command){
        if (command == null) {
            throw new IllegalArgumentException("制造商元数据不能为空");
        }
        if (StringUtils.isBlank(command.getId())) {
            throw new IllegalArgumentException("制造商 ID 不能为空");
        }
        domainManufacturerMetaService.updateManufacturerMeta(command);
    }
    
    public void deleteManufacturerMeta(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        domainManufacturerMetaService.deleteManufacturerMeta(id);
    }
    
    public ManufacturerMeta findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        return domainManufacturerMetaService.findById(id);
    }
}

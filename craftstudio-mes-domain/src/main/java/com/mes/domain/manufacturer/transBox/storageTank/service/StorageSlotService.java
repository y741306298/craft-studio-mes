package com.mes.domain.manufacturer.transBox.storageTank.service;

import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageSlot;
import com.mes.domain.manufacturer.transBox.storageTank.repository.StorageSlotRepository;
import com.mes.domain.shared.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StorageSlotService {

    @Autowired
    private StorageSlotRepository storageSlotRepository;

    public List<StorageSlot> findStorageSlotsByTankId(String storageTankId, int current, int size) {

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException("每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(storageTankId)) {
            throw new BusinessNotAllowException("储存柜 ID 不能为空");
        }

        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("storageTankId", storageTankId);
        return storageSlotRepository.fuzzySearch(searchFilters, current, size);
    }

    public long getTotalCount(String storageTankId) {
        if (StringUtils.isNotBlank(storageTankId)) {
            Map<String, String> searchFilters = new HashMap<>();
            searchFilters.put("storageTankId", storageTankId);
            return storageSlotRepository.totalByFuzzySearch(searchFilters);
        } else {
            return storageSlotRepository.total();
        }
    }

    public StorageSlot addStorageSlot(StorageSlot storageSlot) {
        if (storageSlot == null) {
            throw new BusinessNotAllowException("储位不能为空");
        }
        if (StringUtils.isBlank(storageSlot.getSlotCode())) {
            throw new BusinessNotAllowException("储位编码不能为空");
        }
        if (StringUtils.isBlank(storageSlot.getStorageTankId())) {
            throw new BusinessNotAllowException("所属储存柜 ID 不能为空");
        }

        return storageSlotRepository.add(storageSlot);
    }

    public void updateStorageSlot(StorageSlot storageSlot) {
        if (storageSlot == null) {
            throw new BusinessNotAllowException("储位不能为空");
        }
        if (StringUtils.isBlank(storageSlot.getId())) {
            throw new BusinessNotAllowException("储位 ID 不能为空");
        }

        storageSlotRepository.update(storageSlot);
    }

    public void deleteStorageSlot(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException("ID 不能为空");
        }

        StorageSlot storageSlot = storageSlotRepository.findById(id);
        if (storageSlot != null) {
            storageSlotRepository.delete(storageSlot);
        }
    }

    public StorageSlot findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException("ID 不能为空");
        }
        return storageSlotRepository.findById(id);
    }

    public void storeItem(String slotId, String productionPieceId, String productionPieceType, Integer quantity) {
        if (StringUtils.isBlank(slotId)) {
            throw new BusinessNotAllowException("储位 ID 不能为空");
        }

        StorageSlot slot = findById(slotId);
        if (slot == null) {
            throw new BusinessNotAllowException("储位不存在");
        }

        if (slot.isOccupied()) {
            throw new BusinessNotAllowException("储位已被占用");
        }

        slot.storeItem(productionPieceId, productionPieceType, quantity);
        storageSlotRepository.update(slot);
    }

    public void retrieveItem(String slotId) {
        if (StringUtils.isBlank(slotId)) {
            throw new BusinessNotAllowException("储位 ID 不能为空");
        }

        StorageSlot slot = findById(slotId);
        if (slot == null) {
            throw new BusinessNotAllowException("储位不存在");
        }

        if (!slot.isOccupied()) {
            throw new BusinessNotAllowException("储位未被占用，无法出库");
        }

        slot.retrieveItem();
        storageSlotRepository.update(slot);
    }
}

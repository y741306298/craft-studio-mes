package com.mes.domain.manufacturer.transBox.storageTank.service;

import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageInventory;
import com.mes.domain.manufacturer.transBox.storageTank.repository.StorageInventoryRepository;
import com.mes.domain.shared.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StorageInventoryService {

    @Autowired
    private StorageInventoryRepository storageInventoryRepository;

    public List<StorageInventory> findInventoriesByTankId(String storageTankId, int current, int size) {

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException("每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(storageTankId)) {
            throw new BusinessNotAllowException("储存柜 ID 不能为空");
        }

        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("storageTankId", storageTankId);
        return storageInventoryRepository.fuzzySearch(searchFilters, current, size);
    }

    public long getTotalCount(String storageTankId) {
        if (StringUtils.isNotBlank(storageTankId)) {
            Map<String, String> searchFilters = new HashMap<>();
            searchFilters.put("storageTankId", storageTankId);
            return storageInventoryRepository.totalByFuzzySearch(searchFilters);
        } else {
            return storageInventoryRepository.total();
        }
    }

    public StorageInventory addInventory(StorageInventory inventory) {
        if (inventory == null) {
            throw new BusinessNotAllowException("库存记录不能为空");
        }

        return storageInventoryRepository.add(inventory);
    }

    public void updateInventory(StorageInventory inventory) {
        if (inventory == null) {
            throw new BusinessNotAllowException("库存记录不能为空");
        }
        if (StringUtils.isBlank(inventory.getId())) {
            throw new BusinessNotAllowException("库存记录 ID 不能为空");
        }

        storageInventoryRepository.update(inventory);
    }

    public void deleteInventory(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException("ID 不能为空");
        }

        StorageInventory inventory = storageInventoryRepository.findById(id);
        if (inventory != null) {
            storageInventoryRepository.delete(inventory);
        }
    }

    public StorageInventory findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException("ID 不能为空");
        }
        return storageInventoryRepository.findById(id);
    }
}

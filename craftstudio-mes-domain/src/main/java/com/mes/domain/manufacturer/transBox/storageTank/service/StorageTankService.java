package com.mes.domain.manufacturer.transBox.storageTank.service;

import com.mes.application.dto.resp.ApiResponse;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageTank;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageSlot;
import com.mes.domain.manufacturer.transBox.storageTank.repository.StorageTankRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import com.mes.domain.shared.utils.IdGenerator;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StorageTankService {

    @Autowired
    private StorageTankRepository storageTankRepository;

    public List<StorageTank> findStorageTanksByName(String storageTankName, int current, int size) {

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(storageTankName)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜名称不能为空");
        }

        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("storageTankName", storageTankName);
        return storageTankRepository.fuzzySearch(searchFilters, current, size);
    }

    public long getTotalCount(String storageTankName) {
        if (StringUtils.isNotBlank(storageTankName)) {
            Map<String, String> searchFilters = new HashMap<>();
            searchFilters.put("storageTankName", storageTankName);
            return storageTankRepository.totalByFuzzySearch(searchFilters);
        } else {
            return storageTankRepository.total();
        }
    }

    public StorageTank addStorageTank(StorageTank storageTank) {
        if (storageTank == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜不能为空");
        }
        if (StringUtils.isBlank(storageTank.getStorageTankName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜名称不能为空");
        }
        if (StringUtils.isBlank(storageTank.getStorageTankCode())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜编码不能为空");
        }

        // 生成唯一的 storageTankId
        String storageTankId = IdGenerator.generateId("TANK");
        storageTank.setStorageTankId(storageTankId);

        return storageTankRepository.add(storageTank);
    }

    public void updateStorageTank(StorageTank storageTank) {
        if (storageTank == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜不能为空");
        }
        if (StringUtils.isBlank(storageTank.getId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜 ID 不能为空");
        }

        storageTankRepository.update(storageTank);
    }

    public void deleteStorageTank(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID 不能为空");
        }

        StorageTank storageTank = storageTankRepository.findById(id);
        if (storageTank != null) {
            storageTankRepository.delete(storageTank);
        }
    }

    public StorageTank findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID 不能为空");
        }
        return storageTankRepository.findById(id);
    }

    public StorageSlot allocateSlot(StorageTank storageTank, String productionPieceId, String productionPieceType, Integer quantity) {
        if (storageTank == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜不能为空");
        }

        StorageSlot availableSlot = storageTank.findAvailableSlot();
        if (availableSlot == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜没有可用储位");
        }

        availableSlot.storeItem(productionPieceId, productionPieceType, quantity);
        storageTankRepository.update(storageTank);

        return availableSlot;
    }

    public void retrieveSlot(StorageTank storageTank, String slotId) {
        if (storageTank == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜不能为空");
        }
        if (StringUtils.isBlank(slotId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储位 ID 不能为空");
        }

        StorageSlot slot = storageTank.getStorageSlots().stream()
                .filter(s -> slotId.equals(s.getSlotId()))
                .findFirst()
                .orElseThrow(() -> new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储位不存在"));

        slot.retrieveItem();
        storageTankRepository.update(storageTank);
    }
}

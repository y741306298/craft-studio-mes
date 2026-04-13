package com.mes.domain.manufacturer.transBox.storageTank.service;

import com.mes.application.dto.resp.ApiResponse;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageOperationRecord;
import com.mes.domain.manufacturer.transBox.storageTank.repository.StorageOperationRecordRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StorageOperationRecordService {

    @Autowired
    private StorageOperationRecordRepository storageOperationRecordRepository;

    public List<StorageOperationRecord> findRecordsByTankId(String storageTankId, int current, int size) {

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(storageTankId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜 ID 不能为空");
        }

        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("storageTankId", storageTankId);
        return storageOperationRecordRepository.fuzzySearch(searchFilters, current, size);
    }

    public long getTotalCount(String storageTankId) {
        if (StringUtils.isNotBlank(storageTankId)) {
            Map<String, String> searchFilters = new HashMap<>();
            searchFilters.put("storageTankId", storageTankId);
            return storageOperationRecordRepository.totalByFuzzySearch(searchFilters);
        } else {
            return storageOperationRecordRepository.total();
        }
    }

    public StorageOperationRecord recordOperation(StorageOperationRecord record) {
        if (record == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "操作记录不能为空");
        }
        if (StringUtils.isBlank(record.getOperationType())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "操作类型不能为空");
        }

        record.setOperationTime(new Date());
        return storageOperationRecordRepository.add(record);
    }

    public void deleteRecord(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID 不能为空");
        }

        StorageOperationRecord record = storageOperationRecordRepository.findById(id);
        if (record != null) {
            storageOperationRecordRepository.delete(record);
        }
    }

    public StorageOperationRecord findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID 不能为空");
        }
        return storageOperationRecordRepository.findById(id);
    }
}

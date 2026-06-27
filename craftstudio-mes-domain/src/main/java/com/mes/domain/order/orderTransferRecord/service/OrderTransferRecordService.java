package com.mes.domain.order.orderTransferRecord.service;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.order.orderTransferRecord.entity.OrderTransferRecord;
import com.mes.domain.order.orderTransferRecord.repository.OrderTransferRecordRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderTransferRecordService {

    @Autowired
    private OrderTransferRecordRepository orderTransferRecordRepository;

    public Collection<OrderTransferRecord> batchAdd(List<OrderTransferRecord> records) {
        if (records == null || records.isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "转出订单记录不能为空");
        }
        return orderTransferRecordRepository.batchAdd(records);
    }

    public void batchUpdate(List<OrderTransferRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        orderTransferRecordRepository.batchUpdate(records);
    }

    public List<OrderTransferRecord> findAllTransferRecords() {
        long total = orderTransferRecordRepository.total();
        if (total <= 0) {
            return List.of();
        }
        return orderTransferRecordRepository.list(1, Math.toIntExact(total));
    }

    public List<OrderTransferRecord> findTransferInRecords(String targetId, int current, int size) {
        return findByManufacturerField("targetId", targetId, current, size);
    }

    public long countTransferInRecords(String targetId) {
        return countByManufacturerField("targetId", targetId);
    }

    public List<OrderTransferRecord> findTransferOutRecords(String sourceId, int current, int size) {
        return findByManufacturerField("sourceId", sourceId, current, size);
    }

    public long countTransferOutRecords(String sourceId) {
        return countByManufacturerField("sourceId", sourceId);
    }

    private List<OrderTransferRecord> findByManufacturerField(String fieldName, String manufacturerMetaId, int current, int size) {
        validateQuery(manufacturerMetaId, current, size);
        Map<String, Object> filters = new HashMap<>();
        filters.put(fieldName, manufacturerMetaId);
        return orderTransferRecordRepository.filterList(current, size, filters);
    }

    private long countByManufacturerField(String fieldName, String manufacturerMetaId) {
        validateQuery(manufacturerMetaId, 1, 1);
        Map<String, Object> filters = new HashMap<>();
        filters.put(fieldName, manufacturerMetaId);
        return orderTransferRecordRepository.filterTotal(filters);
    }

    private void validateQuery(String manufacturerMetaId, int current, int size) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "制造商 ID 不能为空");
        }
        if (current <= 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "页码必须大于 0");
        }
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }
    }
}

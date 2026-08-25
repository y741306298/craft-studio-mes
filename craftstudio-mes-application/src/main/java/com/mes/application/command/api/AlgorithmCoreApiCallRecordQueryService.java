package com.mes.application.command.api;

import com.mes.domain.shared.algorithm.entity.AlgorithmCoreApiCallRecord;
import com.mes.domain.shared.algorithm.enums.AlgorithmCoreApiCallType;
import com.mes.domain.shared.algorithm.repository.AlgorithmCoreApiCallRecordRepository;
import org.springframework.stereotype.Service;

@Service
public class AlgorithmCoreApiCallRecordQueryService {
    private final AlgorithmCoreApiCallRecordRepository repository;

    public AlgorithmCoreApiCallRecordQueryService(AlgorithmCoreApiCallRecordRepository repository) {
        this.repository = repository;
    }

    public AlgorithmCoreApiCallRecord findLatest(String type, String sourceId) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type不能为空");
        }
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("sourceId不能为空");
        }
        AlgorithmCoreApiCallType.fromValue(type)
                .orElseThrow(() -> new IllegalArgumentException("不支持的type: " + type));
        return repository.findLatestByTypeAndSourceId(type, sourceId);
    }
}

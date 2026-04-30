package com.mes.domain.manufacturer.typesetting.service;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingSequencePool;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingSequenceUsageType;
import com.mes.domain.manufacturer.typesetting.repository.TypesettingSequencePoolRepository;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TypesettingSequencePoolService {

    private static final int DEFAULT_MAX = 10000;

    @Autowired
    private TypesettingSequencePoolRepository sequencePoolRepository;

    public synchronized int nextSequence(String manufacturerMetaId, TypesettingSequenceUsageType usageType) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new IllegalArgumentException("manufacturerMetaId不能为空");
        }
        if (usageType == null) {
            throw new IllegalArgumentException("usageType不能为空");
        }
        TypesettingSequencePool pool = findByManufacturerAndUsage(manufacturerMetaId, usageType);
        if (pool == null) {
            pool = new TypesettingSequencePool();
            pool.setManufacturerMetaId(manufacturerMetaId);
            pool.setUsageType(usageType.getCode());
            pool.setSequenceArray(buildDefaultArray());
        }

        List<Integer> sequenceArray = pool.getSequenceArray();
        if (sequenceArray == null || sequenceArray.isEmpty()) {
            sequenceArray = buildDefaultArray();
            pool.setSequenceArray(sequenceArray);
        }
        Integer next = sequenceArray.remove(0);
        sequenceArray.add(next);

        sequencePoolRepository.saveOrUpdate(pool);
        return next;
    }

    private TypesettingSequencePool findByManufacturerAndUsage(String manufacturerMetaId, TypesettingSequenceUsageType usageType) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("manufacturerMetaId", manufacturerMetaId);
        filters.put("usageType", usageType.getCode());
        List<TypesettingSequencePool> pools = sequencePoolRepository.filterList(1, 1, filters);
        return pools.isEmpty() ? null : pools.get(0);
    }

    private List<Integer> buildDefaultArray() {
        List<Integer> array = new ArrayList<>(DEFAULT_MAX);
        for (int i = 1; i <= DEFAULT_MAX; i++) {
            array.add(i);
        }
        return array;
    }
}

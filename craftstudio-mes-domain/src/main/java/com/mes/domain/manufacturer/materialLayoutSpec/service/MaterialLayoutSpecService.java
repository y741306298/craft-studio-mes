package com.mes.domain.manufacturer.materialLayoutSpec.service;

import com.mes.domain.manufacturer.materialLayoutSpec.entity.MaterialLayoutSpec;
import com.mes.domain.manufacturer.materialLayoutSpec.repository.MaterialLayoutSpecRepository;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MaterialLayoutSpecService {

    @Autowired
    private MaterialLayoutSpecRepository materialLayoutSpecRepository;

    public MaterialLayoutSpec add(MaterialLayoutSpec spec) {
        return materialLayoutSpecRepository.add(spec);
    }

    public void update(MaterialLayoutSpec spec) {
        materialLayoutSpecRepository.update(spec);
    }

    public void delete(MaterialLayoutSpec spec) {
        materialLayoutSpecRepository.delete(spec);
    }

    public MaterialLayoutSpec findById(String id) {
        return materialLayoutSpecRepository.findById(id);
    }

    public List<MaterialLayoutSpec> list(String materialId, String materialName, long current, int size) {
        return materialLayoutSpecRepository.filterList(current, size, buildFilters(materialId, materialName));
    }

    public long total(String materialId, String materialName) {
        return materialLayoutSpecRepository.filterTotal(buildFilters(materialId, materialName));
    }

    private Map<String, Object> buildFilters(String materialId, String materialName) {
        Map<String, Object> filters = new HashMap<>();
        if (StringUtils.isNotBlank(materialId)) {
            filters.put("materialId", materialId);
        }
        if (StringUtils.isNotBlank(materialName)) {
            filters.put("materialSnapshot.name_like", materialName);
        }
        return filters;
    }
}

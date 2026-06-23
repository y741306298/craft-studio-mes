package com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.service;

import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.entity.ManufacturerMaterialLayoutSpecCfg;
import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.repository.ManufacturerMaterialLayoutSpecCfgRepository;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ManufacturerMaterialLayoutSpecCfgService {

    @Autowired
    private ManufacturerMaterialLayoutSpecCfgRepository repository;

    public ManufacturerMaterialLayoutSpecCfg add(ManufacturerMaterialLayoutSpecCfg cfg) {
        return repository.add(cfg);
    }

    public void update(ManufacturerMaterialLayoutSpecCfg cfg) {
        repository.update(cfg);
    }

    public void delete(ManufacturerMaterialLayoutSpecCfg cfg) {
        repository.delete(cfg);
    }

    public ManufacturerMaterialLayoutSpecCfg findById(String id) {
        return repository.findById(id);
    }

    public List<ManufacturerMaterialLayoutSpecCfg> list(String manufacturerMetaId, String materialLayoutSpecId, long current, int size) {
        return repository.filterList(current, size, buildFilters(manufacturerMetaId, materialLayoutSpecId));
    }

    public long total(String manufacturerMetaId, String materialLayoutSpecId) {
        return repository.filterTotal(buildFilters(manufacturerMetaId, materialLayoutSpecId));
    }

    private Map<String, Object> buildFilters(String manufacturerMetaId, String materialLayoutSpecId) {
        Map<String, Object> filters = new HashMap<>();
        if (StringUtils.isNotBlank(manufacturerMetaId)) {
            filters.put("manufacturerMetaId", manufacturerMetaId);
        }
        if (StringUtils.isNotBlank(materialLayoutSpecId)) {
            filters.put("materialLayoutSpecId", materialLayoutSpecId);
        }
        return filters;
    }
}

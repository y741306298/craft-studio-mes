package com.mes.domain.manufacturer.typesetting.service;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingContainerWidthInset;
import com.mes.domain.manufacturer.typesetting.repository.TypesettingContainerWidthInsetRepository;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TypesettingContainerWidthInsetService {

    @Autowired
    private TypesettingContainerWidthInsetRepository containerWidthInsetRepository;

    public TypesettingContainerWidthInset findByMaterialIdAndLayoutMode(String materialId, String layoutMode) {
        if (StringUtils.isBlank(materialId) || StringUtils.isBlank(layoutMode)) {
            return null;
        }
        Map<String, Object> filters = new HashMap<>();
        filters.put("materialId", materialId);
        filters.put("layoutMode", layoutMode);
        List<TypesettingContainerWidthInset> insets = containerWidthInsetRepository.filterList(1, 1, filters);
        return insets.isEmpty() ? null : insets.get(0);
    }
}

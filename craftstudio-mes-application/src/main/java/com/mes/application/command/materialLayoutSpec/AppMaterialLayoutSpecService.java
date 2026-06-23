package com.mes.application.command.materialLayoutSpec;

import com.mes.domain.manufacturer.materialLayoutSpec.entity.MaterialLayoutSpec;
import com.mes.domain.manufacturer.materialLayoutSpec.entity.MaterialLayoutSpecStep;
import com.mes.domain.manufacturer.materialLayoutSpec.service.MaterialLayoutSpecService;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AppMaterialLayoutSpecService {

    @Autowired
    private MaterialLayoutSpecService materialLayoutSpecService;

    public PagedResult<MaterialLayoutSpec> list(String materialId, String materialName, PagedQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        List<MaterialLayoutSpec> items = materialLayoutSpecService.list(materialId, materialName, query.getCurrent(), query.getSize());
        long total = materialLayoutSpecService.total(materialId, materialName);
        return new PagedResult<>(items, total, query.getSize(), query.getCurrent());
    }

    public MaterialLayoutSpec add(MaterialLayoutSpec spec) {
        validate(spec);
        return materialLayoutSpecService.add(spec);
    }

    public void update(MaterialLayoutSpec spec) {
        if (StringUtils.isBlank(spec.getId())) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        validate(spec);
        materialLayoutSpecService.update(spec);
    }

    public void delete(String id) {
        MaterialLayoutSpec spec = findById(id);
        materialLayoutSpecService.delete(spec);
    }

    public MaterialLayoutSpec findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        return materialLayoutSpecService.findById(id);
    }

    private void validate(MaterialLayoutSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("材料排版规格不能为空");
        }
        if (StringUtils.isBlank(spec.getMaterialId())) {
            throw new IllegalArgumentException("materialId不能为空");
        }
        if (spec.getInsetSteps() == null || spec.getInsetSteps().isEmpty()) {
            throw new IllegalArgumentException("阶梯数据不能为空");
        }
        long validStepCount = spec.getInsetSteps().stream()
                .filter(Objects::nonNull)
                .map(MaterialLayoutSpecStep::getMaxLengthMeter)
                .filter(meter -> meter != null && meter >= 1 && meter <= 10)
                .distinct()
                .count();
        if (validStepCount != 10) {
            throw new IllegalArgumentException("阶梯数据必须包含1m到10m的内缩配置");
        }
    }
}

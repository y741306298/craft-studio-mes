package com.mes.application.command.manufacturerMaterialLayoutSpecCfg;

import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.entity.ManufacturerMaterialLayoutSpecCfg;
import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.service.ManufacturerMaterialLayoutSpecCfgService;
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
public class AppManufacturerMaterialLayoutSpecCfgService {

    @Autowired
    private ManufacturerMaterialLayoutSpecCfgService cfgService;

    @Autowired
    private MaterialLayoutSpecService materialLayoutSpecService;

    public PagedResult<ManufacturerMaterialLayoutSpecCfg> list(String manufacturerMetaId, String materialId, PagedQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        List<ManufacturerMaterialLayoutSpecCfg> items = cfgService.list(manufacturerMetaId, materialId, query.getCurrent(), query.getSize());
        long total = cfgService.total(manufacturerMetaId, materialId);
        return new PagedResult<>(items, total, query.getSize(), query.getCurrent());
    }

    public ManufacturerMaterialLayoutSpecCfg add(ManufacturerMaterialLayoutSpecCfg cfg) {
        validate(cfg);
        fillMaterialSnapshotIfAbsent(cfg);
        return cfgService.add(cfg);
    }

    public void update(ManufacturerMaterialLayoutSpecCfg cfg) {
        if (StringUtils.isBlank(cfg.getId())) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        validate(cfg);
        fillMaterialSnapshotIfAbsent(cfg);
        cfgService.update(cfg);
    }

    public void delete(String id) {
        cfgService.delete(findById(id));
    }

    public ManufacturerMaterialLayoutSpecCfg findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        return cfgService.findById(id);
    }

    /**
     * 校验工厂材料步进配置。
     * <p>
     * 工厂角色直接选择一个可配置材料后，在这条工厂+材料配置上维护 1m 到 10m 的阶梯内缩值。
     * 因此步进信息跟随 manufacturerMetaId + materialId 保存，而不是先配到材料再绑定工厂。
     */
    private void validate(ManufacturerMaterialLayoutSpecCfg cfg) {
        if (cfg == null) {
            throw new IllegalArgumentException("工厂材料排版规格配置不能为空");
        }
        if (StringUtils.isBlank(cfg.getManufacturerMetaId())) {
            throw new IllegalArgumentException("manufacturerMetaId不能为空");
        }
        if (StringUtils.isBlank(cfg.getMaterialId())) {
            throw new IllegalArgumentException("materialId不能为空");
        }
        if (!materialExists(cfg.getMaterialId())) {
            throw new IllegalArgumentException("可配置材料不存在");
        }
        if (cfg.getInsetSteps() == null || cfg.getInsetSteps().isEmpty()) {
            throw new IllegalArgumentException("阶梯数据不能为空");
        }
        // 只接受 1m 到 10m 的阶梯上限，并按 maxLengthMeter 去重统计。
        long validStepCount = cfg.getInsetSteps().stream()
                .filter(Objects::nonNull)
                .map(MaterialLayoutSpecStep::getMaxLengthMeter)
                .filter(meter -> meter != null && meter >= 1 && meter <= 10)
                .distinct()
                .count();
        if (validStepCount != 10) {
            throw new IllegalArgumentException("阶梯数据必须包含1m到10m的内缩配置");
        }
    }

    private boolean materialExists(String materialId) {
        return materialLayoutSpecService.total(materialId, null) > 0;
    }

    private void fillMaterialSnapshotIfAbsent(ManufacturerMaterialLayoutSpecCfg cfg) {
        if (cfg.getMaterialSnapshot() != null && cfg.getUsageSize3D() != null) {
            return;
        }
        List<MaterialLayoutSpec> specs = materialLayoutSpecService.list(cfg.getMaterialId(), null, 1, 1);
        if (specs.isEmpty()) {
            return;
        }
        MaterialLayoutSpec spec = specs.get(0);
        if (cfg.getMaterialSnapshot() == null) {
            cfg.setMaterialSnapshot(spec.getMaterialSnapshot());
        }
        if (cfg.getUsageSize3D() == null) {
            cfg.setUsageSize3D(spec.getUsageSize3D());
        }
    }
}

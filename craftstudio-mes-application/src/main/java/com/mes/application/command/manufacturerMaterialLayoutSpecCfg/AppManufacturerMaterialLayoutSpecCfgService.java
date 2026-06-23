package com.mes.application.command.manufacturerMaterialLayoutSpecCfg;

import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.entity.ManufacturerMaterialLayoutSpecCfg;
import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.service.ManufacturerMaterialLayoutSpecCfgService;
import com.mes.domain.manufacturer.materialLayoutSpec.service.MaterialLayoutSpecService;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppManufacturerMaterialLayoutSpecCfgService {

    @Autowired
    private ManufacturerMaterialLayoutSpecCfgService cfgService;

    @Autowired
    private MaterialLayoutSpecService materialLayoutSpecService;

    public PagedResult<ManufacturerMaterialLayoutSpecCfg> list(String manufacturerMetaId, String materialLayoutSpecId, PagedQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        List<ManufacturerMaterialLayoutSpecCfg> items = cfgService.list(manufacturerMetaId, materialLayoutSpecId, query.getCurrent(), query.getSize());
        long total = cfgService.total(manufacturerMetaId, materialLayoutSpecId);
        return new PagedResult<>(items, total, query.getSize(), query.getCurrent());
    }

    public ManufacturerMaterialLayoutSpecCfg add(ManufacturerMaterialLayoutSpecCfg cfg) {
        validate(cfg);
        return cfgService.add(cfg);
    }

    public void update(ManufacturerMaterialLayoutSpecCfg cfg) {
        if (StringUtils.isBlank(cfg.getId())) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        validate(cfg);
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

    private void validate(ManufacturerMaterialLayoutSpecCfg cfg) {
        if (cfg == null) {
            throw new IllegalArgumentException("工厂材料排版规格配置不能为空");
        }
        if (StringUtils.isBlank(cfg.getManufacturerMetaId())) {
            throw new IllegalArgumentException("manufacturerMetaId不能为空");
        }
        if (StringUtils.isBlank(cfg.getMaterialLayoutSpecId())) {
            throw new IllegalArgumentException("materialLayoutSpecId不能为空");
        }
        if (materialLayoutSpecService.findById(cfg.getMaterialLayoutSpecId()) == null) {
            throw new IllegalArgumentException("材料排版规格不存在");
        }
    }
}

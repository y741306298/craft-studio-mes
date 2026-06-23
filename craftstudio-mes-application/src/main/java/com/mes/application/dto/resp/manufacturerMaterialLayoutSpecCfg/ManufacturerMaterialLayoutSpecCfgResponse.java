package com.mes.application.dto.resp.manufacturerMaterialLayoutSpecCfg;

import com.mes.application.dto.resp.materialLayoutSpec.MaterialLayoutSpecResponse;
import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.entity.ManufacturerMaterialLayoutSpecCfg;
import lombok.Data;

import java.util.Date;

@Data
public class ManufacturerMaterialLayoutSpecCfgResponse {
    private String id;
    private Date createTime;
    private Date updateTime;
    private String manufacturerMetaId;
    private String materialLayoutSpecId;
    private MaterialLayoutSpecResponse materialLayoutSpec;

    public static ManufacturerMaterialLayoutSpecCfgResponse from(
            ManufacturerMaterialLayoutSpecCfg cfg,
            MaterialLayoutSpecResponse materialLayoutSpec) {
        if (cfg == null) {
            return null;
        }
        ManufacturerMaterialLayoutSpecCfgResponse response = new ManufacturerMaterialLayoutSpecCfgResponse();
        response.setId(cfg.getId());
        response.setCreateTime(cfg.getCreateTime());
        response.setUpdateTime(cfg.getUpdateTime());
        response.setManufacturerMetaId(cfg.getManufacturerMetaId());
        response.setMaterialLayoutSpecId(cfg.getMaterialLayoutSpecId());
        response.setMaterialLayoutSpec(materialLayoutSpec);
        return response;
    }
}

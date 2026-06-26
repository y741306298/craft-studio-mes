package com.mes.application.dto.resp.manufacturerMaterialLayoutSpecCfg;

import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.entity.ManufacturerMaterialLayoutSpecCfg;
import com.mes.domain.manufacturer.materialLayoutSpec.entity.MaterialLayoutSpecStep;
import com.mes.domain.order.orderInfo.vo.MaterialConfig;
import com.piliofpala.craftstudio.shared.domain.graphics.vo.Size3D;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ManufacturerMaterialLayoutSpecCfgResponse {
    private String id;
    private Date createTime;
    private Date updateTime;
    private String manufacturerMetaId;
    private String materialId;
    private MaterialConfig.MaterialSnapshot materialSnapshot;
    private Size3D usageSize3D;
    private List<MaterialLayoutSpecStep> insetSteps;

    public static ManufacturerMaterialLayoutSpecCfgResponse from(ManufacturerMaterialLayoutSpecCfg cfg) {
        if (cfg == null) {
            return null;
        }
        ManufacturerMaterialLayoutSpecCfgResponse response = new ManufacturerMaterialLayoutSpecCfgResponse();
        response.setId(cfg.getId());
        response.setCreateTime(cfg.getCreateTime());
        response.setUpdateTime(cfg.getUpdateTime());
        response.setManufacturerMetaId(cfg.getManufacturerMetaId());
        response.setMaterialId(cfg.getMaterialId());
        response.setMaterialSnapshot(cfg.getMaterialSnapshot());
        response.setUsageSize3D(cfg.getUsageSize3D());
        response.setInsetSteps(cfg.getInsetSteps());
        return response;
    }
}

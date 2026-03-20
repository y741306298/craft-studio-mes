package com.mes.interfaces.api.dto.resp.manufacturerMeta;

import com.mes.domain.base.UnitPrice;
import com.mes.domain.manufacturer.enums.CfgStatus;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.vo.ManufacturerMtsProductSpec;
import lombok.Data;

@Data
public class ManufacturerMtsProductSpecSummary {

    private String id;
    private String name;
    private String previewUrl;
    private Integer materialCount;  // 材料数量
    private Integer procedureCount; // 工序数量
    private Boolean customizable;
    private UnitPrice price;
    private String status;
    private String statusName;

    /**
     * 从 ManufacturerMtsProductSpec 实体转换为响应 DTO
     * @param spec 规格实体
     * @return ManufacturerMtsProductSpecSummary
     */
    public static ManufacturerMtsProductSpecSummary from(ManufacturerMtsProductSpec spec) {
        if (spec == null) {
            return null;
        }

        ManufacturerMtsProductSpecSummary response = new ManufacturerMtsProductSpecSummary();
        response.setId(spec.getId());
        response.setName(spec.getName());
        response.setPreviewUrl(spec.getPreviewUrl());
        response.setMaterialCount(spec.getMaterials() != null ? spec.getMaterials().size() : 0);
        response.setProcedureCount(spec.getProcedureFlow() != null ? spec.getProcedureFlow().size() : 0);
        response.setCustomizable(spec.getCustomizable());
        response.setPrice(spec.getPrice());
        response.setStatus(spec.getStatus() != null ? spec.getStatus().getCode() : null);
        response.setStatusName(spec.getStatus() != null ? spec.getStatus().getDescription() : null);

        return response;
    }
}

package com.mes.application.dto.resp.materialLayoutSpec;

import com.mes.domain.manufacturer.materialLayoutSpec.entity.MaterialLayoutSpec;
import com.mes.domain.order.orderInfo.vo.MaterialConfig;
import com.piliofpala.craftstudio.shared.domain.graphics.vo.Size3D;
import lombok.Data;

import java.util.Date;

@Data
public class MaterialLayoutSpecResponse {
    private String id;
    private Date createTime;
    private Date updateTime;
    private String materialId;
    private MaterialConfig.MaterialSnapshot materialSnapshot;
    private Size3D usageSize3D;

    public static MaterialLayoutSpecResponse from(MaterialLayoutSpec spec) {
        if (spec == null) {
            return null;
        }
        MaterialLayoutSpecResponse response = new MaterialLayoutSpecResponse();
        response.setId(spec.getId());
        response.setCreateTime(spec.getCreateTime());
        response.setUpdateTime(spec.getUpdateTime());
        response.setMaterialId(spec.getMaterialId());
        response.setMaterialSnapshot(spec.getMaterialSnapshot());
        response.setUsageSize3D(spec.getUsageSize3D());
        return response;
    }
}

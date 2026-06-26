package com.mes.application.dto.req.materialLayoutSpec;

import com.mes.domain.manufacturer.materialLayoutSpec.entity.MaterialLayoutSpec;
import com.mes.domain.order.orderInfo.vo.MaterialConfig;
import com.piliofpala.craftstudio.shared.domain.graphics.vo.Size3D;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MaterialLayoutSpecRequest {
    private String id;

    @NotBlank(message = "materialId不能为空")
    private String materialId;

    private MaterialConfig.MaterialSnapshot materialSnapshot;

    private Size3D usageSize3D;

    public MaterialLayoutSpec toDomainEntity() {
        MaterialLayoutSpec spec = new MaterialLayoutSpec();
        spec.setId(id);
        spec.setMaterialId(materialId);
        spec.setMaterialSnapshot(materialSnapshot);
        spec.setUsageSize3D(usageSize3D);
        return spec;
    }
}

package com.mes.application.dto.req.materialLayoutSpec;

import com.mes.domain.manufacturer.materialLayoutSpec.entity.MaterialLayoutSpec;
import com.mes.domain.manufacturer.materialLayoutSpec.entity.MaterialLayoutSpecStep;
import com.mes.domain.order.orderInfo.vo.MaterialConfig;
import com.piliofpala.craftstudio.shared.domain.graphics.vo.Size3D;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MaterialLayoutSpecRequest {
    private String id;

    @NotBlank(message = "materialId不能为空")
    private String materialId;

    private MaterialConfig.MaterialSnapshot materialSnapshot;

    private Size3D usageSize3D;

    @NotEmpty(message = "阶梯数据不能为空")
    private List<MaterialLayoutSpecStep> insetSteps;

    public MaterialLayoutSpec toDomainEntity() {
        MaterialLayoutSpec spec = new MaterialLayoutSpec();
        spec.setId(id);
        spec.setMaterialId(materialId);
        spec.setMaterialSnapshot(materialSnapshot);
        spec.setUsageSize3D(usageSize3D);
        spec.setInsetSteps(insetSteps);
        return spec;
    }
}

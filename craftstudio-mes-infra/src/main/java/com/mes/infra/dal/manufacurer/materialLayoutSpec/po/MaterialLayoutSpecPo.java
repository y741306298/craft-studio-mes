package com.mes.infra.dal.manufacurer.materialLayoutSpec.po;

import com.mes.domain.manufacturer.materialLayoutSpec.entity.MaterialLayoutSpec;
import com.mes.domain.order.orderInfo.vo.MaterialConfig;
import com.mes.infra.base.BasePO;
import com.piliofpala.craftstudio.shared.domain.graphics.vo.Size3D;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "material_layout_spec")
public class MaterialLayoutSpecPo extends BasePO<MaterialLayoutSpec> {

    private String materialId;
    private MaterialConfig.MaterialSnapshot materialSnapshot;
    private Size3D usageSize3D;

    @Override
    public MaterialLayoutSpec toDO() {
        MaterialLayoutSpec spec = new MaterialLayoutSpec();
        copyBaseFieldsToDO(spec);
        spec.setMaterialId(materialId);
        spec.setMaterialSnapshot(materialSnapshot);
        spec.setUsageSize3D(usageSize3D);
        return spec;
    }

    @Override
    protected BasePO<MaterialLayoutSpec> fromDO(MaterialLayoutSpec spec) {
        this.materialId = spec.getMaterialId();
        this.materialSnapshot = spec.getMaterialSnapshot();
        this.usageSize3D = spec.getUsageSize3D();
        return this;
    }
}

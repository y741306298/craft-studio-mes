package com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.materialLayoutSpec.entity.MaterialLayoutSpecStep;
import com.mes.domain.order.orderInfo.vo.MaterialConfig;
import com.piliofpala.craftstudio.shared.domain.graphics.vo.Size3D;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ManufacturerMaterialLayoutSpecCfg extends BaseEntity {
    /**
     * 制造商元数据 ID。
     */
    private String manufacturerMetaId;

    /**
     * 工厂角色选中的可配置材料 ID，参考 MaterialLayoutSpec.materialId。
     */
    private String materialId;

    /**
     * 工厂配置步进信息时的材料快照，参考 orderItem.material.materialSnapshot。
     */
    private MaterialConfig.MaterialSnapshot materialSnapshot;

    /**
     * 工厂配置步进信息时的材料使用尺寸，参考 orderItem.material.usageSize3D。
     */
    private Size3D usageSize3D;

    /**
     * 跟随工厂和材料组合保存的 1m 到 10m 阶梯内缩数据。
     */
    private List<MaterialLayoutSpecStep> insetSteps;
}

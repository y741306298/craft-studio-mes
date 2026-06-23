package com.mes.domain.manufacturer.materialLayoutSpec.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.order.orderInfo.vo.MaterialConfig;
import com.piliofpala.craftstudio.shared.domain.graphics.vo.Size3D;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class MaterialLayoutSpec extends BaseEntity {
    /**
     * 材料 ID，参考 orderItem.material.materialId。
     */
    private String materialId;

    /**
     * 材料快照，参考 orderItem.material.materialSnapshot。
     */
    private MaterialConfig.MaterialSnapshot materialSnapshot;

    /**
     * 材料使用尺寸，参考 orderItem.material.usageSize3D。
     */
    private Size3D usageSize3D;

    /**
     * 1m 到 10m 的阶梯内缩数据。
     */
    private List<MaterialLayoutSpecStep> insetSteps;
}

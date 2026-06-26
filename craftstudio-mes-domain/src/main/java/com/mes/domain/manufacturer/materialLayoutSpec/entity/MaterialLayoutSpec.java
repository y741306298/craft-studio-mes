package com.mes.domain.manufacturer.materialLayoutSpec.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.order.orderInfo.vo.MaterialConfig;
import com.piliofpala.craftstudio.shared.domain.graphics.vo.Size3D;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MaterialLayoutSpec extends BaseEntity {
    /**
     * 可配置材料 ID，参考 orderItem.material.materialId。
     * <p>
     * 该配置只声明“哪些材料可以被工厂角色选择后配置步进信息”，不保存工厂侧步进内缩规则。
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
}

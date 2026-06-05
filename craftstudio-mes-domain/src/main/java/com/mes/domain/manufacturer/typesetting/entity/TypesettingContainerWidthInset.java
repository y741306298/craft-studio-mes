package com.mes.domain.manufacturer.typesetting.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TypesettingContainerWidthInset extends BaseEntity {
    /**
     * 材料 ID。
     */
    private String materialId;

    /**
     * 排版方式。
     */
    private String layoutMode;

    /**
     * containers.width 排版前需要扣减的内缩值（mm）。
     */
    private Integer widthInset;
}

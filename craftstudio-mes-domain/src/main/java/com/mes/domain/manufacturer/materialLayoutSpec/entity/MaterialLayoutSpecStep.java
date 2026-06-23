package com.mes.domain.manufacturer.materialLayoutSpec.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialLayoutSpecStep {
    /**
     * 长度阶梯上限（米），例如 1 表示 1m 内。
     */
    private Integer maxLengthMeter;

    /**
     * 内缩值（厘米）。
     */
    private BigDecimal insetCm;
}

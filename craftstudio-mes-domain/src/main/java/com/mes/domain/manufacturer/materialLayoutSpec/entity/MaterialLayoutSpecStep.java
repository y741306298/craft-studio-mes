package com.mes.domain.manufacturer.materialLayoutSpec.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialLayoutSpecStep {
    /**
     * 长度阶梯上限（米），支持小数，例如 0.1 表示 0.1m 内。
     */
    private BigDecimal maxLengthMeter;

    /**
     * 内缩值（厘米）。
     */
    private BigDecimal insetCm;
}

package com.mes.domain.manufacturer.typesetting.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TypesettingElement {

    /**
     * 算法输出的排版 svg 路径，如 test/nest/nested1.svg
     */
    private String nestedSvg;
    /**
     * 排版利用率，如 0.636334
     */
    private BigDecimal utilization;
    /**
     * 排版结果宽度
     */
    private BigDecimal width;
    /**
     * 排版结果高度
     */
    private BigDecimal height;
}

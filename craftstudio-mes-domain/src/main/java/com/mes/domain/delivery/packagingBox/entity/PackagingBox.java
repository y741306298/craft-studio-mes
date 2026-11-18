package com.mes.domain.delivery.packagingBox.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class PackagingBox extends BaseEntity {
    private String boxName;
    private String code;
    private BigDecimal cardboardDepth;
    private BigDecimal cardboardHeight;
    private BigDecimal cardboardWidth;
    private BigDecimal boxLength;
    private BigDecimal boxWidth;
    private BigDecimal height;
    private String cuttingKnife;
    private String rollingCutter;
    private String grindingKnife;
}

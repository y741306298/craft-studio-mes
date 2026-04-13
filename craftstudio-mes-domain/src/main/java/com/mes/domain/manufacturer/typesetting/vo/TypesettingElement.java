package com.mes.domain.manufacturer.typesetting.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TypesettingElement {

    private String nestedSvg;
    private BigDecimal utilization;
}

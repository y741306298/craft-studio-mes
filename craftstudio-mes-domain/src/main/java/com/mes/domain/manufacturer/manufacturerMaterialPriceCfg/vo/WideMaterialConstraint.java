package com.mes.domain.manufacturer.manufacturerMaterialPriceCfg.vo;

import lombok.Data;

@Data
public class WideMaterialConstraint {
    private float minTotalWidth;
    private float maxTotalWidth;
    private float minTotalHeight;
    private float maxTotalHeight;

    public boolean valid() {
        return minTotalWidth>0 && maxTotalWidth>0 && minTotalHeight>0 && maxTotalHeight>0;
    }
}

package com.mes.application.dto.req.packagingBox;

import com.mes.domain.delivery.packagingBox.entity.PackagingBox;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PackagingBoxRequest {
    private String id;
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

    public PackagingBox toDomainEntity() {
        PackagingBox box = new PackagingBox();
        box.setId(id);
        box.setBoxName(boxName);
        box.setCode(code);
        box.setCardboardDepth(cardboardDepth);
        box.setCardboardHeight(cardboardHeight);
        box.setCardboardWidth(cardboardWidth);
        box.setBoxLength(boxLength);
        box.setBoxWidth(boxWidth);
        box.setHeight(height);
        box.setCuttingKnife(cuttingKnife);
        box.setRollingCutter(rollingCutter);
        box.setGrindingKnife(grindingKnife);
        return box;
    }
}

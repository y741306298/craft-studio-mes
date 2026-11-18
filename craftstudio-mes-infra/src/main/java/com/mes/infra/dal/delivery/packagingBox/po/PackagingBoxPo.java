package com.mes.infra.dal.delivery.packagingBox.po;

import com.mes.domain.delivery.packagingBox.entity.PackagingBox;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "packaging_box")
public class PackagingBoxPo extends BasePO<PackagingBox> {
    private String boxName;
    private String code;
    private String manufacturerId;
    @Field(targetType = FieldType.STRING)
    private BigDecimal cardboardDepth;
    @Field(targetType = FieldType.STRING)
    private BigDecimal cardboardHeight;
    @Field(targetType = FieldType.STRING)
    private BigDecimal cardboardWidth;
    @Field(targetType = FieldType.STRING)
    private BigDecimal boxLength;
    @Field(targetType = FieldType.STRING)
    private BigDecimal boxWidth;
    @Field(targetType = FieldType.STRING)
    private BigDecimal height;
    private String cuttingKnife;
    private String rollingCutter;
    private String grindingKnife;
    private List<String> cardboardHeightHistory;
    private List<String> cardboardWidthHistory;

    @Override
    public PackagingBox toDO() {
        PackagingBox box = new PackagingBox();
        copyBaseFieldsToDO(box);
        box.setBoxName(boxName);
        box.setCode(code);
        box.setManufacturerId(manufacturerId);
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

    @Override
    protected PackagingBoxPo fromDO(PackagingBox box) {
        this.boxName = box.getBoxName();
        this.code = box.getCode();
        this.manufacturerId = box.getManufacturerId();
        this.cardboardDepth = box.getCardboardDepth();
        this.cardboardHeight = box.getCardboardHeight();
        this.cardboardWidth = box.getCardboardWidth();
        this.boxLength = box.getBoxLength();
        this.boxWidth = box.getBoxWidth();
        this.height = box.getHeight();
        this.cuttingKnife = box.getCuttingKnife();
        this.rollingCutter = box.getRollingCutter();
        this.grindingKnife = box.getGrindingKnife();
        this.cardboardHeightHistory = cardboardHeight == null ? null : List.of(cardboardHeight.toString());
        this.cardboardWidthHistory = cardboardWidth == null ? null : List.of(cardboardWidth.toString());
        return this;
    }
}

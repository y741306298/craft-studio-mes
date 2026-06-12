package com.mes.application.command.typesetting.enums;

/**
 * 印版生成算法元素类型。
 */
public enum FormeGenerationElementType {
    /** 圆形定位点。 */
    ANCHOR_POINT_CIRCLE("SP9", "定位点：圆"),
    /** 十字架定位点。 */
    ANCHOR_POINT_CROSS("SP7", "定位点：十字架"),
    /** 震动刀。 */
    VIBRATION_KNIFE("SP2", "震动刀"),
    /** 拖刀。 */
    DRAG_KNIFE("SP3", "拖刀");

    private final String code;
    private final String description;

    FormeGenerationElementType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

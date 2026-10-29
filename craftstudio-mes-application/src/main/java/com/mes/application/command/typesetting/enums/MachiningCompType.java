package com.mes.application.command.typesetting.enums;

/** 印版生成算法加工对象类型编码。 */
public enum MachiningCompType {
    /** 印版，预留类型。 */
    FORME("FORME"),
    /** 零件，目前支持的加工对象。 */
    PART("PART");

    private final String code;

    MachiningCompType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

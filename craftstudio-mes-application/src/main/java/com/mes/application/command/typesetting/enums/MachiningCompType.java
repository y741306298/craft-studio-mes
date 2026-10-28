package com.mes.application.command.typesetting.enums;

/** 印版生成算法加工对象类型编码。 */
public enum MachiningCompType {
    /** 当前协议要求传递完整字面值 forme|part。 */
    FORME_OR_PART("forme|part");

    private final String code;

    MachiningCompType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

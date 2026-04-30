package com.mes.domain.manufacturer.typesetting.enums;

public enum TypesettingSequenceUsageType {
    LAYOUT_ID("layout_id", "排版ID生成"),
    PLT_FILE_NAME("plt_file_name", "印版PLT文件名生成");

    private final String code;
    private final String desc;

    TypesettingSequenceUsageType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static TypesettingSequenceUsageType parseCode(String code) {
        for (TypesettingSequenceUsageType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知 usageType: " + code);
    }
}

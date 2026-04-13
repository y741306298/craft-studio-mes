package com.mes.application.command.typesetting.enums;

public enum TypesettingSourceType {
    TYPESETTING("TYPESETTING", "排版文件"),
    PART("PRODUCTION_PIECE", "零件");

    private final String code;
    private final String description;

    TypesettingSourceType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TypesettingSourceType fromDescription(String description) {
        for (TypesettingSourceType type : values()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的查询类型：" + description);
    }

    public static TypesettingSourceType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (TypesettingSourceType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}

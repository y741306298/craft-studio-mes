package com.mes.application.command.typesetting.enums;

/**
 * 排版查询类型枚举类
 */
public enum TypesettingQueryType {
    ALL("ALL", "全部"),
    PART("PART", "零件"),
    TYPESETTING("TYPESETTING", "排版");

    private final String code;
    private final String description;

    TypesettingQueryType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TypesettingQueryType fromDescription(String description) {
        for (TypesettingQueryType type : values()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的查询类型：" + description);
    }

    public static TypesettingQueryType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (TypesettingQueryType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}

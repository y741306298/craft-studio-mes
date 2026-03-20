package com.mes.application.command.typesetting.enums;

/**
 * 排版状态枚举类
 */
public enum TypesettingStatus {
    TYPESITTING("排版中"),
    PENDING_CONFIRM("待确认"),
    CONFIRMED("已确认"),
    PENDING_PRINT("待打印");

    private final String description;

    TypesettingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static TypesettingStatus fromDescription(String description) {
        for (TypesettingStatus status : values()) {
            if (status.getDescription().equals(description)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的排版状态：" + description);
    }
}

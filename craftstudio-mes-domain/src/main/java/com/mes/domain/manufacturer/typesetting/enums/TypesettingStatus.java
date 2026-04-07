package com.mes.domain.manufacturer.typesetting.enums;

/**
 * 排版状态枚举
 */
public enum TypesettingStatus {

    PENDING("待排版", "pending"),           // 等待排版
    IN_PROGRESS("排版中", "in_progress"),   // 正在排版
    CONFIRMING("确认中", "confirming"),     // 等待确认
    COMPLETED("已下达", "completed");       // 已下达

    private final String description;
    private final String code;

    TypesettingStatus(String description, String code) {
        this.description = description;
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }

    /**
     * 根据 code 获取枚举
     */
    public static TypesettingStatus getByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        for (TypesettingStatus status : values()) {
            if (status.getCode().equalsIgnoreCase(code)) {
                return status;
            }
        }

        return null;
    }

    /**
     * 根据中文名称获取枚举
     */
    public static TypesettingStatus getByChineseName(String chineseName) {
        if (chineseName == null || chineseName.trim().isEmpty()) {
            return null;
        }

        for (TypesettingStatus status : values()) {
            if (status.getDescription().equals(chineseName)) {
                return status;
            }
        }

        return null;
    }
}

package com.mes.domain.manufacturer.typesetting.enums;

/**
 * 排版状态枚举
 */
public enum TypesettingStatus {

    PENDING("待排版", "pending"),           // 等待排版
    IN_PROGRESS("排版中", "in_progress"),   // 正在排版
    CONFIRMING("待确认", "confirming"),     // 等待确认
    CONFIRM_IN_PROGRESS("确认中", "confirm_in_progress"),
    DISPATCH_IN_PROGRESS("下发中", "dispatch_in_progress"),
    CONFIRMED("已确认", "confirmed"),
    PRINTING("待打印", "printing"),
    PRINTING_IN_PROGRESS("打印中", "printing_in_progress"),
    COMPLETED("已完成", "completed"),
    FAILED("处理失败", "failed");       // 已下达

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

    public static boolean isFormeInProgress(String code) {
        return CONFIRM_IN_PROGRESS.getCode().equals(code) || DISPATCH_IN_PROGRESS.getCode().equals(code);
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

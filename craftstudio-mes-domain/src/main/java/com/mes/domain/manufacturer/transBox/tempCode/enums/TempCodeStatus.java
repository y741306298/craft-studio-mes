package com.mes.domain.manufacturer.transBox.tempCode.enums;

/**
 * 临时码状态枚举
 */
public enum TempCodeStatus {
    UNUSED("未使用"),      // 未被使用
    PENDING_USE("待使用"), // 等待使用
    IN_USE("使用中");      // 正在使用

    private final String description;

    TempCodeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据描述获取状态枚举
     *
     * @param description 状态描述
     * @return 对应的状态枚举
     * @throws IllegalArgumentException 如果描述不匹配任何状态
     */
    public static TempCodeStatus fromDescription(String description) {
        for (TempCodeStatus status : values()) {
            if (status.getDescription().equals(description)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的临时码状态：" + description);
    }
}

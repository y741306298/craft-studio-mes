package com.mes.domain.manufacturer.device.enums;

/**
 * 设备类型枚举
 */
public enum DeviceType {
    
    PRINT("印刷类", "print"),           // 印刷类设备
    CUTTING("切割类", "cutting"),       // 切割类设备
    FUBAN("覆板类", "fuban");           // 覆板类设备

    private final String chineseName;
    private final String code;

    DeviceType(String chineseName, String code) {
        this.chineseName = chineseName;
        this.code = code;
    }

    public String getChineseName() {
        return chineseName;
    }

    public String getCode() {
        return code;
    }

    /**
     * 根据 code 获取枚举
     */
    public static DeviceType getByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        for (DeviceType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }

        return null;
    }

    /**
     * 根据中文名称获取枚举
     */
    public static DeviceType getByChineseName(String chineseName) {
        if (chineseName == null || chineseName.trim().isEmpty()) {
            return null;
        }

        for (DeviceType type : values()) {
            if (type.getChineseName().equals(chineseName)) {
                return type;
            }
        }

        return null;
    }
}

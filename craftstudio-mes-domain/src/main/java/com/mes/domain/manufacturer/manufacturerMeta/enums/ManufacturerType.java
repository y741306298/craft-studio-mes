package com.mes.domain.manufacturer.manufacturerMeta.enums;

/**
 * 制造商类型枚举
 */
public enum ManufacturerType {
    
    BASIC_PRINT("基础印刷工厂", "basic_print"),      // 基础印刷工厂
    STANDARD_PRODUCT("标品工厂", "standard_product"), // 标品工厂
    CHARACTER_CARD("字牌工厂", "character_card");     // 字牌工厂

    private final String description;
    private final String code;

    ManufacturerType(String description, String code) {
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
    public static ManufacturerType getByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        for (ManufacturerType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }

        return null;
    }

    /**
     * 根据中文名称获取枚举
     */
    public static ManufacturerType getByChineseName(String chineseName) {
        if (chineseName == null || chineseName.trim().isEmpty()) {
            return null;
        }

        for (ManufacturerType type : values()) {
            if (type.getDescription().equals(chineseName)) {
                return type;
            }
        }

        return null;
    }
}

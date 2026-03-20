package com.mes.domain.manufacturer.enums;

public enum CfgStatus {
    NORMAL("NORMAL", "正常"),
    INVALID("INVALID", "失效");

    private final String code;
    private final String description;

    CfgStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据 code 获取枚举实例
     * @param code 状态码
     * @return 对应的枚举实例，如果未找到则返回 null
     */
    public static CfgStatus getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (CfgStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}

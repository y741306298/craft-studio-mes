package com.mes.domain.order.enums;

/**
 * 订单状态枚举
 */
public enum OrderStatus {
    PENDING("PENDING", "待处理"),
    IN_PRODUCTION("IN_PRODUCTION", "生产中"),
    PACKAGED("PACKAGED", "已打包"),
    FAILED("FAILED", "处理失败");

    private final String code;
    private final String description;

    OrderStatus(String code, String description) {
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
    public static OrderStatus getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}

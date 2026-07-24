package com.mes.domain.order.enums;

/**
 * 订单渠道类型枚举。
 */
public enum OrderChannelType {
    GATHER_PLATFORM("GATHER_PLATFORM", "聚单平台单"),
    MANUAL("MANUAL", "手工单");

    private final String code;
    private final String description;

    OrderChannelType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static OrderChannelType getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (OrderChannelType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}

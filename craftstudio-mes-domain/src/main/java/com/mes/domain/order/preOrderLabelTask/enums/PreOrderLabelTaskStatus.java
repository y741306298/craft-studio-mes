package com.mes.domain.order.preOrderLabelTask.enums;

/**
 * 预下快递单批处理任务状态枚举。
 */
public enum PreOrderLabelTaskStatus {
    PENDING("PENDING", "待处理"),
    PROCESSED("PROCESSED", "已处理");

    private final String code;
    private final String description;

    PreOrderLabelTaskStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

package com.mes.domain.manufacturer.procedureFlow.enums;

/**
 * 流程状态枚举
 */
public enum FlowStatus {
    
    DRAFT("草稿"),          // 草稿状态
    NOT_STARTED("未开始"),   // 尚未开始
    RUNNING("运行中"),       // 正在运行
    SUSPENDED("已暂停"),     // 暂停
    COMPLETED("已完成"),     // 正常完成
    FAILED("失败"),         // 执行失败
    CANCELLED("已取消");    // 已取消
    
    private final String description;
    
    FlowStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断是否为终态
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}

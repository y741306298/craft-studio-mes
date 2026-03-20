package com.mes.domain.manufacturer.procedureFlow.enums;

/**
 * 节点状态枚举
 */
public enum NodeStatus {
    
    PENDING("待处理"),      // 等待执行
    ACTIVE("执行中"),       // 正在执行
    COMPLETED("已完成"),    // 正常完成
    FAILED("失败"),         // 执行失败
    SKIPPED("已跳过"),      // 被跳过
    CANCELLED("已取消");    // 已取消
    
    private final String description;
    
    NodeStatus(String description) {
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

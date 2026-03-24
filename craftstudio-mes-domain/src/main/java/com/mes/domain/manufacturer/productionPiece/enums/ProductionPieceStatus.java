package com.mes.domain.manufacturer.productionPiece.enums;

/**
 * 生产工件状态机枚举
 */
public enum ProductionPieceStatus {
    PROCESSING("PROCESSING", "处理中"),
    PENDING_TYPESITTING("PENDING_TYPESITTING", "待排版"),
    TYPESITTING("TYPESITTING", "排版中"),
    TYPESITTING_PENDING_CONFIRM("TYPESITTING_PENDING_CONFIRM", "排版待确认"),
    PENDING_PRINT("PENDING_PRINT", "待打印"),
    PRINTING("PRINTING", "打印中"),
    PENDING_CUTTING("PENDING_CUTTING", "待切割"),
    CUTTING("CUTTING", "切割中"),
    PENDING_FUBAN("PENDING_FUBAN", "待覆板"),
    FUBAN("FUBAN", "覆板中"),
    PENDING_PACKING("PENDING_PACKING", "待打包"),
    PACKING_COMPLETED("PACKING_COMPLETED", "打包完成"),
    RETURNED("RETURNED", "已退单");

    private final String code;
    private final String description;

    ProductionPieceStatus(String code, String description) {
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
    public static ProductionPieceStatus getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (ProductionPieceStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据描述获取状态枚举
     *
     * @param description 状态描述
     * @return 对应的状态枚举
     * @throws IllegalArgumentException 如果描述不匹配任何状态
     */
    public static ProductionPieceStatus fromDescription(String description) {
        for (ProductionPieceStatus status : values()) {
            if (status.getDescription().equals(description)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的生产工件状态：" + description);
    }

    /**
     * 判断是否为终态
     *
     * @return true 如果是终态，否则返回 false
     */
    public boolean isFinalState() {
        return this == PACKING_COMPLETED || this == RETURNED;
    }

    /**
     * 判断是否可以进行排版操作
     *
     * @return true 如果可以进行排版，否则返回 false
     */
    public boolean canTypeset() {
        return this == PENDING_TYPESITTING || this == PROCESSING;
    }

    /**
     * 判断是否可以进行打印操作
     *
     * @return true 如果可以进行打印，否则返回 false
     */
    public boolean canPrint() {
        return this == PENDING_PRINT || this == TYPESITTING_PENDING_CONFIRM;
    }

    /**
     * 判断是否可以进行切割操作
     *
     * @return true 如果可以进行切割，否则返回 false
     */
    public boolean canCut() {
        return this == PENDING_CUTTING || this == PRINTING;
    }

    /**
     * 判断是否可以进行覆板操作
     *
     * @return true 如果可以进行覆板，否则返回 false
     */
    public boolean canFuBan() {
        return this == PENDING_FUBAN || this == CUTTING;
    }

    /**
     * 判断是否可以进行打包操作
     *
     * @return true 如果可以进行打包，否则返回 false
     */
    public boolean canPack() {
        return this == PENDING_PACKING || this == FUBAN;
    }
}

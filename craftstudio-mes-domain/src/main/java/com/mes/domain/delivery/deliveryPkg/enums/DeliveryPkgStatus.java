package com.mes.domain.delivery.deliveryPkg.enums;

/**
 * 包裹状态机枚举
 */
public enum DeliveryPkgStatus {
    PENDING_PACKING("待打包"),
    PACKING("打包中"),
    PENDING_DELIVERY("待发货"),
    DELIVERED("已发货");

    private final String description;

    DeliveryPkgStatus(String description) {
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
    public static DeliveryPkgStatus fromDescription(String description) {
        for (DeliveryPkgStatus status : values()) {
            if (status.getDescription().equals(description)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的包裹状态：" + description);
    }

    /**
     * 判断是否为终态
     *
     * @return true 如果是终态，否则返回 false
     */
    public boolean isFinalState() {
        return this == DELIVERED;
    }

    /**
     * 判断是否可以进行打包操作
     *
     * @return true 如果可以进行打包，否则返回 false
     */
    public boolean canPack() {
        return this == PENDING_PACKING;
    }

    /**
     * 判断是否可以进行发货操作
     *
     * @return true 如果可以进行发货，否则返回 false
     */
    public boolean canDeliver() {
        return this == PACKING || this == PENDING_DELIVERY;
    }

    /**
     * 判断是否可以开始打包
     *
     * @return true 如果可以开始打包，否则返回 false
     */
    public boolean canStartPacking() {
        return this == PENDING_PACKING;
    }

    /**
     * 判断是否可以完成打包
     *
     * @return true 如果可以完成打包，否则返回 false
     */
    public boolean canCompletePacking() {
        return this == PACKING;
    }
}

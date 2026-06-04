package com.mes.application.command.typesetting.proces.buckle;

/**
 * mask SVG 中一个扣点的中心坐标与业务后缀。
 */
public class BuckleMarkPoint {
    private final String suffix;
    private final double centerX;
    private final double centerY;

    public BuckleMarkPoint(String suffix, double centerX, double centerY) {
        this.suffix = suffix;
        this.centerX = centerX;
        this.centerY = centerY;
    }

    public String getSuffix() {
        return suffix;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterY() {
        return centerY;
    }
}

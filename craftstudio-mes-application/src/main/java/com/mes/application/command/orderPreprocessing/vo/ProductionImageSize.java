package com.mes.application.command.orderPreprocessing.vo;

/** 生产图物理尺寸，单位为毫米；不改变订单原始材料尺寸。 */
public record ProductionImageSize(Double widthMm, Double heightMm) {
    private static final double RECT_BLOOD_BORDER_MM = 2D;

    public ProductionImageSize withRectBlood(String scrollEdge, double scrollWidthMm, double sewWidthMm) {
        if (widthMm == null || heightMm == null || !Double.isFinite(widthMm) || !Double.isFinite(heightMm)
                || widthMm <= 0 || heightMm <= 0 || !Double.isFinite(scrollWidthMm)
                || !Double.isFinite(sewWidthMm) || scrollWidthMm < 0 || sewWidthMm < 0) {
            throw new IllegalArgumentException("矩形缝套出血需要有效原始宽高及非负外扩距离");
        }
        // 黑边位于外扩后矩形的四周，各边再增加 2mm。
        double border = RECT_BLOOD_BORDER_MM * 2;
        return switch (scrollEdge) {
            case "LEFT", "RIGHT" -> new ProductionImageSize(
                    widthMm + scrollWidthMm + sewWidthMm + border,
                    heightMm + sewWidthMm * 2 + border);
            case "TOP", "BOTTOM" -> new ProductionImageSize(
                    widthMm + sewWidthMm * 2 + border,
                    heightMm + scrollWidthMm + sewWidthMm + border);
            default -> throw new IllegalArgumentException("矩形旗缝套筒选边必须为 LEFT/TOP/BOTTOM/RIGHT");
        };
    }
}

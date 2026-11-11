package com.mes.application.command.orderPreprocessing.sublimation;

/** 溯源号仅放在缝套指定外扩边的末端；同边有工艺文字时留5mm间隔；不增加画布尺寸。 */
public record SublimationTracePlacement(SublimationGeometry.Edge edge, SublimationGeometry.Margins margins,
                                        double offsetX, double offsetY, double processMaxWidth,
                                        double processMaxHeight) {
    public static SublimationTracePlacement create(double width, double height, SublimationGeometry.Edge sleeveEdge,
                                                  double textLength, double band, boolean sharesProcessEdge) {
        if (sleeveEdge == null) return null;
        SublimationGeometry.Edge selected = sleeveEdge;
        boolean horizontal = horizontal(selected);
        double length = (horizontal ? width : height) - 4;
        double offset = length - 20 - textLength;
        if (!Double.isFinite(textLength) || textLength <= 0 || offset < 0) {
            throw new IllegalArgumentException("热升华矩形尺寸不足以按当前字号完整显示订单溯源号");
        }
        var margins = new SublimationGeometry.Margins(selected == SublimationGeometry.Edge.LEFT ? band : 0,
                selected == SublimationGeometry.Edge.TOP ? band : 0,
                selected == SublimationGeometry.Edge.RIGHT ? band : 0,
                selected == SublimationGeometry.Edge.BOTTOM ? band : 0);
        return new SublimationTracePlacement(selected, margins, horizontal ? offset : 0, horizontal ? 0 : offset,
                width - 4 - (sharesProcessEdge && horizontal ? textLength + 5 : 0),
                height - 4 - (sharesProcessEdge && !horizontal ? textLength + 5 : 0));
    }
    private static boolean horizontal(SublimationGeometry.Edge edge) {
        return edge == SublimationGeometry.Edge.TOP || edge == SublimationGeometry.Edge.BOTTOM;
    }
}

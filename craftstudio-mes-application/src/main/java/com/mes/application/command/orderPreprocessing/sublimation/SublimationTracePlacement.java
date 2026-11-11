package com.mes.application.command.orderPreprocessing.sublimation;

/** 溯源号放在一条外扩边的末端，与该边的工艺文字留5mm间隔；不增加画布尺寸。 */
public record SublimationTracePlacement(SublimationGeometry.Edge edge, SublimationGeometry.Margins margins,
                                        double offsetX, double offsetY, double processMaxWidth,
                                        double processMaxHeight) {
    public static SublimationTracePlacement create(double width, double height, SublimationGeometry.Margins available,
                                                  double textLength, double band) {
        SublimationGeometry.Edge selected = null;
        double longest = -1;
        for (var edge : SublimationGeometry.Edge.values()) {
            if (available.at(edge) <= 0) continue;
            double length = horizontal(edge) ? width : height;
            if (length > longest) { selected = edge; longest = length; }
        }
        // 无缝套也无锁边时，溯源号仍需输出，放在下边黑线内侧。
        if (selected == null) selected = SublimationGeometry.Edge.BOTTOM;
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
                width - 4 - (horizontal ? textLength + 5 : 0),
                height - 4 - (horizontal ? 0 : textLength + 5));
    }
    private static boolean horizontal(SublimationGeometry.Edge edge) {
        return edge == SublimationGeometry.Edge.TOP || edge == SublimationGeometry.Edge.BOTTOM;
    }
}

package com.mes.application.command.orderPreprocessing.sublimation;

import java.util.ArrayList;
import java.util.List;

/** 热升华矩形旗的物理尺寸及标记点。坐标、边长和外扩值均使用毫米。 */
public final class SublimationGeometry {
    public static final double BORDER_MM = 2D;
    public enum Edge { LEFT, TOP, BOTTOM, RIGHT }
    public enum MarkKind { BUCKLE, TIE }
    public record Point(String key, MarkKind kind, double x, double y) { }
    public record Size(double width, double height) { }
    /** 不包含最外圈黑边的四边工艺外扩量。 */
    public record Margins(double left, double top, double right, double bottom) {
        public double at(Edge edge) {
            return switch (edge) {
                case LEFT -> left;
                case TOP -> top;
                case RIGHT -> right;
                case BOTTOM -> bottom;
            };
        }
        public Size expand(double width, double height) {
            requireSize(width, height);
            return new Size(width + left + right + BORDER_MM * 2,
                    height + top + bottom + BORDER_MM * 2);
        }
    }

    private SublimationGeometry() { }

    public static Margins margins(Edge sleeveEdge, double sleeveWidth, double sewWidth) {
        if (!Double.isFinite(sleeveWidth) || !Double.isFinite(sewWidth) || sleeveWidth < 0 || sewWidth < 0) {
            throw new IllegalArgumentException("热升华工艺外扩量必须为非负毫米数");
        }
        return new Margins(sleeveEdge == Edge.LEFT ? sleeveWidth : sewWidth,
                sleeveEdge == Edge.TOP ? sleeveWidth : sewWidth,
                sleeveEdge == Edge.RIGHT ? sleeveWidth : sewWidth,
                sleeveEdge == Edge.BOTTOM ? sleeveWidth : sewWidth);
    }

    /** 中间点数量不含两个端点，阈值本身归入较小的区间。 */
    public static int intermediatePointCount(double edgeLengthMm) {
        if (!Double.isFinite(edgeLengthMm) || edgeLengthMm <= 0) {
            throw new IllegalArgumentException("旗帜边长必须为正数");
        }
        if (edgeLengthMm <= 2000) return 1;
        if (edgeLengthMm <= 3500) return 2;
        if (edgeLengthMm <= 4500) return 3;
        if (edgeLengthMm <= 6000) return 4;
        return Math.addExact(4, (int) Math.ceil((edgeLengthMm - 6000) / 1000D));
    }

    /** 四角扣点的中心距相邻两边均为 20mm。 */
    public static List<Point> corners(double width, double height) {
        requireSize(width, height);
        if (width < 40 || height < 40) throw new IllegalArgumentException("旗帜尺寸不足以容纳四角20mm扣点");
        return List.of(new Point("corner-lt", MarkKind.BUCKLE, 20, 20),
                new Point("corner-rt", MarkKind.BUCKLE, width - 20, 20),
                new Point("corner-rb", MarkKind.BUCKLE, width - 20, height - 20),
                new Point("corner-lb", MarkKind.BUCKLE, 20, height - 20));
    }

    /** 沿指定边内缩 30mm；两端点距相邻边 50mm，中间点在端点间等分。 */
    public static List<Point> edgePoints(Edge edge, MarkKind kind, double width, double height) {
        requireSize(width, height);
        boolean horizontal = edge == Edge.TOP || edge == Edge.BOTTOM;
        double length = horizontal ? width : height;
        double perpendicular = horizontal ? height : width;
        if (length <= 100 || perpendicular < 60) {
            throw new IllegalArgumentException("旗帜尺寸不足以按边距30mm、端距50mm布置标记点");
        }
        int middle = intermediatePointCount(length);
        List<Point> result = new ArrayList<>();
        for (int i = 0; i <= middle + 1; i++) {
            double along = 50D + (length - 100D) * i / (middle + 1);
            double x = horizontal ? along : edge == Edge.LEFT ? 30D : width - 30D;
            double y = horizontal ? edge == Edge.TOP ? 30D : height - 30D : along;
            result.add(new Point(edge.name().toLowerCase(java.util.Locale.ROOT) + "-" + i, kind, x, y));
        }
        return List.copyOf(result);
    }

    private static void requireSize(double width, double height) {
        if (!Double.isFinite(width) || !Double.isFinite(height) || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("热升华矩形宽高必须为正毫米数");
        }
    }
}

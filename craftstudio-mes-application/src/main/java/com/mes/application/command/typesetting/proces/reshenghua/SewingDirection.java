package com.mes.application.command.typesetting.proces.reshenghua;

import java.util.Set;

/** 热升华缝边方向，节点名称精确匹配。 */
public enum SewingDirection {
    TOP_BOTTOM("上下缝", Edge.TOP, Edge.BOTTOM),
    LEFT_RIGHT("左右缝", Edge.LEFT, Edge.RIGHT),
    TOP("上边缝", Edge.TOP),
    BOTTOM("下边缝", Edge.BOTTOM),
    LEFT("左边缝", Edge.LEFT),
    RIGHT("右边缝", Edge.RIGHT);

    public enum Edge { TOP, BOTTOM, LEFT, RIGHT }

    private final String nodeName;
    private final Set<Edge> edges;

    SewingDirection(String nodeName, Edge... edges) {
        this.nodeName = nodeName;
        this.edges = Set.of(edges);
    }

    public Set<Edge> edges() {
        return edges;
    }

    public static SewingDirection match(String nodeName) {
        for (SewingDirection direction : values()) {
            if (direction.nodeName.equals(nodeName == null ? null : nodeName.trim())) {
                return direction;
            }
        }
        return null;
    }
}

package com.mes.application.command.orderPreprocessing.sublimation;

import java.util.List;
import java.util.Set;

/** 将各后续工艺合并为一次矩形出血和一次矩形零件装饰所需的数据。 */
public record SublimationPlan(SublimationGeometry.Edge sleeveEdge, int sleeveWidthMm, int sewWidthMm,
                              boolean cornerBuckles, Set<SublimationGeometry.Edge> buckleEdges,
                              Set<SublimationGeometry.Edge> tieEdges, SublimationDoubleSideJoin.Mode doubleSideMode,
                              String backImageUrl, List<String> processNames) {
    public SublimationPlan {
        if (sleeveWidthMm < 0 || sewWidthMm < 0 || (sleeveWidthMm > 0 && sleeveEdge == null)) {
            throw new IllegalArgumentException("热升华缝套/锁边配置无效");
        }
        buckleEdges = Set.copyOf(buckleEdges);
        tieEdges = Set.copyOf(tieEdges);
        processNames = List.copyOf(processNames);
    }

    public SublimationGeometry.Margins margins() {
        return SublimationGeometry.margins(sleeveEdge, sleeveWidthMm, sewWidthMm);
    }

    /** 有锁边时标记锁边外扩区域，否则只在套筒外扩区域标记。 */
    public SublimationGeometry.Margins labelMargins() {
        if (sewWidthMm > 0) {
            return new SublimationGeometry.Margins(sleeveEdge == SublimationGeometry.Edge.LEFT ? 0 : sewWidthMm,
                    sleeveEdge == SublimationGeometry.Edge.TOP ? 0 : sewWidthMm,
                    sleeveEdge == SublimationGeometry.Edge.RIGHT ? 0 : sewWidthMm,
                    sleeveEdge == SublimationGeometry.Edge.BOTTOM ? 0 : sewWidthMm);
        }
        return margins();
    }

    public boolean differentBack() {
        return doubleSided() && backImageUrl != null && !backImageUrl.isBlank();
    }

    public boolean doubleSided() {
        return doubleSideMode != SublimationDoubleSideJoin.Mode.NONE;
    }
}

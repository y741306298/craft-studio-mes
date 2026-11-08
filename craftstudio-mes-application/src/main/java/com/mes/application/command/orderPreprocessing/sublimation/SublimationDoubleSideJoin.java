package com.mes.application.command.orderPreprocessing.sublimation;

/** 双面缝及双面夹黑缝的拼合扩展入口；当前保持前后面独立，不执行拼合或材料校验。 */
public interface SublimationDoubleSideJoin {
    enum Mode { NONE, DOUBLE_SEWN, BLACK_INTERLAYER }

    record Input(String frontUrl, String backUrl, SublimationGeometry.Edge frontSleeveEdge,
                 SublimationGeometry.Edge backSleeveEdge, double gapMm, Mode mode) { }

    record Result(String frontUrl, String backUrl, boolean joined) { }

    Result prepare(Input input);

    /** 当前占位实现，后续可注入实际的拼合算法。 */
    static SublimationDoubleSideJoin separateImages() {
        return input -> new Result(input.frontUrl(), input.backUrl(), false);
    }
}

package com.mes.application.command.orderPreprocessing.splice;

/**
 * 固定出血值拼接工艺策略基类。
 *
 * <p>背胶/写真/覆板/喷绘/无痕/板材等拼接在算法调用前的处理流程一致，
 * 只在工艺节点名称和默认出血值上存在差异。</p>
 */
public abstract class AbstractFixedBloodSpliceProcessStrategy implements AlgorithmSpliceProcessStrategy {
    private final String nodeName;
    private final int bloodMm;
    private final boolean allowCoordinateBloodOverride;

    protected AbstractFixedBloodSpliceProcessStrategy(String nodeName, int bloodMm) {
        this(nodeName, bloodMm, false);
    }

    protected AbstractFixedBloodSpliceProcessStrategy(String nodeName, int bloodMm, boolean allowCoordinateBloodOverride) {
        this.nodeName = nodeName;
        this.bloodMm = bloodMm;
        this.allowCoordinateBloodOverride = allowCoordinateBloodOverride;
    }

    @Override
    public String nodeName() {
        return nodeName;
    }

    @Override
    public int bloodMm() {
        return bloodMm;
    }

    @Override
    public int resolveBloodMm(Integer coordinateBloodMm) {
        if (allowCoordinateBloodOverride && coordinateBloodMm != null) {
            return coordinateBloodMm;
        }
        return bloodMm;
    }
}

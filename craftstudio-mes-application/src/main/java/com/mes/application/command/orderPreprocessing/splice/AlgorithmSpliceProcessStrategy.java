package com.mes.application.command.orderPreprocessing.splice;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import io.micrometer.common.util.StringUtils;

/**
 * 调用算法前的拼接工艺策略。
 *
 * <p>当前策略负责把不同“拼接”工艺节点转换成算法 {@code slice} 参数中的默认出血值。
 * 算法回调后的生产工件处理可沿用同样的策略拆分方式扩展独立的后处理策略。</p>
 */
public interface AlgorithmSpliceProcessStrategy {

    /** @return 当前策略精确匹配的工艺节点名称。 */
    String nodeName();

    /** @return 当前拼接工艺传给算法的默认出血值，单位 mm。 */
    int bloodMm();

    /**
     * 解析单个坐标最终传给算法的出血值，单位 mm。
     *
     * <p>“超幅拼接”为兼容既有逻辑，可继续使用坐标自身 blood；新增拼接工艺默认使用策略固定值。</p>
     */
    int resolveBloodMm(Integer coordinateBloodMm);

    default boolean matches(ProcedureFlowNode node) {
        return node != null && StringUtils.isNotBlank(node.getNodeName()) && nodeName().equals(node.getNodeName());
    }
}

package com.mes.application.command.orderPreprocessing.splice;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 拼接工艺策略集合。
 *
 * <p>算法调用前统一从这里识别所有会生成 {@code slice} 的拼接工艺；
 * 后续若要扩展算法回调后的处理，可在同包增加后处理策略集合，保持前后处理职责独立。</p>
 */
public final class SpliceProcessStrategies {
    private static final List<AlgorithmSpliceProcessStrategy> DEFAULT_STRATEGIES = List.of(
            new SuperWidthSpliceProcessStrategy(),
            new AdhesiveSpliceProcessStrategy(),
            new PhotoSpliceProcessStrategy(),
            new BoardCoverSpliceProcessStrategy(),
            new InkjetSpliceProcessStrategy(),
            new SeamlessSpliceProcessStrategy(),
            new PanelSpliceProcessStrategy()
    );

    private SpliceProcessStrategies() {
    }

    public static List<AlgorithmSpliceProcessStrategy> defaults() {
        return DEFAULT_STRATEGIES;
    }

    public static boolean hasSpliceNode(ProcedureFlow procedureFlow) {
        return procedureFlow != null && procedureFlow.getNodes() != null
                && procedureFlow.getNodes().stream().anyMatch(node -> findByNode(node).isPresent());
    }

    public static Optional<ProcedureFlowNode> findLastSpliceNode(ProcedureFlow procedureFlow) {
        return procedureFlow == null ? Optional.empty() : findLastSpliceNode(procedureFlow.getNodes());
    }

    public static Optional<ProcedureFlowNode> findLastSpliceNode(List<ProcedureFlowNode> nodes) {
        return findLastSpliceNode(nodes, DEFAULT_STRATEGIES);
    }

    public static Optional<ProcedureFlowNode> findLastSpliceNode(List<ProcedureFlowNode> nodes, List<AlgorithmSpliceProcessStrategy> spliceStrategies) {
        if (nodes == null || nodes.isEmpty()) {
            return Optional.empty();
        }
        List<AlgorithmSpliceProcessStrategy> effectiveStrategies = spliceStrategies == null || spliceStrategies.isEmpty()
                ? DEFAULT_STRATEGIES
                : spliceStrategies;
        return nodes.stream()
                .filter(node -> findByNode(node, effectiveStrategies).isPresent())
                .max(Comparator
                        .comparingInt((ProcedureFlowNode node) -> node.getNodeOrder() == null ? Integer.MIN_VALUE : node.getNodeOrder())
                        .thenComparingInt(nodes::indexOf));
    }

    public static Optional<AlgorithmSpliceProcessStrategy> findLastStrategy(ProcedureFlow procedureFlow) {
        return findLastSpliceNode(procedureFlow).flatMap(SpliceProcessStrategies::findByNode);
    }

    public static Optional<AlgorithmSpliceProcessStrategy> findByNode(ProcedureFlowNode node) {
        return findByNode(node, DEFAULT_STRATEGIES);
    }

    public static Optional<AlgorithmSpliceProcessStrategy> findByNode(ProcedureFlowNode node, List<AlgorithmSpliceProcessStrategy> spliceStrategies) {
        List<AlgorithmSpliceProcessStrategy> effectiveStrategies = spliceStrategies == null || spliceStrategies.isEmpty()
                ? DEFAULT_STRATEGIES
                : spliceStrategies;
        return effectiveStrategies.stream().filter(strategy -> strategy.matches(node)).findFirst();
    }

    public static String nodeNamesText() {
        return DEFAULT_STRATEGIES.stream()
                .map(AlgorithmSpliceProcessStrategy::nodeName)
                .collect(Collectors.joining("/"));
    }
}

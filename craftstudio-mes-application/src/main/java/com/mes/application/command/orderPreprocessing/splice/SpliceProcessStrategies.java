package com.mes.application.command.orderPreprocessing.splice;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.Blood;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** 订单预处理中使用的拼接工艺策略和切线参数。 */
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

    /** 回调的出血坐标缺失或均为零时，使用拼接工艺配置的切线判定横切。 */
    public static boolean isHorizontalCut(ProcedureFlow procedureFlow, Blood blood) {
        Integer x = blood == null ? null : blood.getX();
        Integer y = blood == null ? null : blood.getY();
        if ((x != null && x != 0) || (y != null && y != 0)) {
            return Integer.valueOf(0).equals(x) && y != null && y != 0;
        }
        return findLastSpliceNode(procedureFlow)
                .map(SpliceProcessStrategies::coordinates)
                .map(coordinates -> coordinates.xs().isEmpty() && !coordinates.ys().isEmpty())
                .orElse(false);
    }

    public static SpliceCoordinates coordinates(ProcedureFlowNode node) {
        List<Object> xs = new ArrayList<>();
        List<Object> ys = new ArrayList<>();
        if (node != null && node.getParamConfigs() != null) {
            for (MTOProductSpecDTO.ProcessParamConfigDTO config : node.getParamConfigs()) {
                if (config != null) {
                    collectCoordinates(config.getParam(), xs, ys);
                }
            }
        }
        return new SpliceCoordinates(xs, ys);
    }

    private static void collectCoordinates(Object param, List<Object> xs, List<Object> ys) {
        if (param == null) {
            return;
        }
        if (param instanceof Map<?, ?> map) {
            addCoordinates(xs, map.get("xs"));
            addCoordinates(ys, map.get("ys"));
        } else if (param instanceof List<?> pairs) {
            for (int i = 0; i + 1 < pairs.size(); i += 2) {
                xs.add(pairs.get(i));
                ys.add(pairs.get(i + 1));
            }
        } else {
            addCoordinates(xs, getCoordinates(param, "getXs"));
            addCoordinates(ys, getCoordinates(param, "getYs"));
        }
    }

    private static void addCoordinates(List<Object> target, Object value) {
        if (value instanceof List<?> values) {
            target.addAll(values);
        }
    }

    private static Object getCoordinates(Object param, String getterName) {
        try {
            return param.getClass().getMethod(getterName).invoke(param);
        } catch (Exception ignored) {
            return null;
        }
    }

    public record SpliceCoordinates(List<Object> xs, List<Object> ys) {
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

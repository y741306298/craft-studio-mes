package com.mes.application.command.orderPreprocessing.splice;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpliceProcessStrategiesTest {

    @Test
    void findLastStrategyReturnsLastSpliceNodeByFlowOrder() {
        ProcedureFlow procedureFlow = new ProcedureFlow();
        procedureFlow.setNodes(List.of(
                node("背胶拼接", null),
                node("写真拼接", null),
                node("覆板拼接", null)
        ));

        assertThat(SpliceProcessStrategies.findLastStrategy(procedureFlow))
                .isPresent()
                .get()
                .extracting(AlgorithmSpliceProcessStrategy::nodeName)
                .isEqualTo("覆板拼接");
    }

    @Test
    void findLastStrategyUsesNodeOrderWhenPresent() {
        ProcedureFlow procedureFlow = new ProcedureFlow();
        procedureFlow.setNodes(List.of(
                node("板材拼接", 20),
                node("喷绘拼接", 10),
                node("无痕拼接", 30)
        ));

        assertThat(SpliceProcessStrategies.findLastStrategy(procedureFlow))
                .isPresent()
                .get()
                .extracting(AlgorithmSpliceProcessStrategy::nodeName)
                .isEqualTo("无痕拼接");
    }

    @Test
    void findLastStrategyIncludesSuperWidthSplice() {
        ProcedureFlow procedureFlow = new ProcedureFlow();
        procedureFlow.setNodes(List.of(
                node("喷绘拼接", null),
                node("超幅拼接", null)
        ));

        assertThat(SpliceProcessStrategies.findLastStrategy(procedureFlow))
                .isPresent()
                .get()
                .extracting(AlgorithmSpliceProcessStrategy::nodeName)
                .isEqualTo("超幅拼接");
    }

    private ProcedureFlowNode node(String nodeName, Integer nodeOrder) {
        ProcedureFlowNode node = new ProcedureFlowNode();
        node.setNodeName(nodeName);
        node.setNodeOrder(nodeOrder);
        return node;
    }
}

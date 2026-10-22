package com.mes.domain.manufacturer.procedureFlow.util;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProcedureFlowNodeMatcherTest {
    @Test
    void sublimationFabricDoesNotTriggerDoubleSideRules() {
        for (String name : List.of("热升华（1.6米双面喷夹黑布）", "热升华(1.6米双面喷夹黑布)")) {
            assertFalse(ProcedureFlowNodeMatcher.isDoubleSideMountingNodeName(name));
            assertFalse(ProcedureFlowNodeMatcher.hasDoubleSideMountingNode(flow(name)));
            assertFalse(ProcedureFlowNodeMatcher.hasAnyNodeNameContaining(flow(name), "双面喷"));
            assertFalse(ProcedureFlowNodeMatcher.containsDoubleSideMountingKeyword("打印 → " + name + " → 裁切"));
            assertTrue(ProcedureFlowNodeMatcher.hasAnyNodeNameContaining(flow(name), "热升华"));
        }
    }

    @Test
    void independentDoubleSideProcessesStillMatch() {
        for (String name : List.of("双面喷", "双面喷印", "双面对裱", "覆双面")) {
            assertTrue(ProcedureFlowNodeMatcher.isDoubleSideMountingNodeName(name));
            assertTrue(ProcedureFlowNodeMatcher.hasDoubleSideMountingNode(flow(name)));
            assertTrue(ProcedureFlowNodeMatcher.hasDoubleSideMountingNode(flow("热升华（1.6米双面喷夹黑布）", name)));
            assertTrue(ProcedureFlowNodeMatcher.containsDoubleSideMountingKeyword("热升华（1.6米双面喷夹黑布） → " + name));
        }
    }

    @Test
    void doubleSideTapeRemainsExcluded() {
        for (String name : List.of("反面覆双面胶", "覆超薄海绵双面胶", "覆双面胶")) {
            assertFalse(ProcedureFlowNodeMatcher.isDoubleSideMountingNodeName(name));
            assertFalse(ProcedureFlowNodeMatcher.hasDoubleSideMountingNode(flow(name)));
            assertFalse(ProcedureFlowNodeMatcher.containsDoubleSideMountingKeyword(name));
        }
    }

    private ProcedureFlow flow(String... names) {
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(java.util.Arrays.stream(names).map(name -> {
            ProcedureFlowNode node = new ProcedureFlowNode();
            node.setNodeName(name);
            return node;
        }).toList());
        return flow;
    }
}

package com.mes.domain.manufacturer.procedureFlow.util;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcedureFlowNodeMatcherTest {

    private static final List<String> EXCLUDED_DOUBLE_SIDE_TAPE_NAMES = List.of(
            "反面覆双面胶",
            "覆超薄海绵双面胶",
            "覆双面胶"
    );

    @Test
    void shouldExcludeDoubleSideTapeNamesFromAllMatchers() {
        for (String nodeName : EXCLUDED_DOUBLE_SIDE_TAPE_NAMES) {
            assertFalse(ProcedureFlowNodeMatcher.hasDoubleSideMountingNode(flowWithNode(nodeName)), nodeName);
            assertFalse(ProcedureFlowNodeMatcher.isDoubleSideMountingNodeName(nodeName), nodeName);
            assertFalse(ProcedureFlowNodeMatcher.containsDoubleSideMountingKeyword(nodeName), nodeName);
        }
    }

    @Test
    void shouldStillMatchDoubleSideProcesses() {
        for (String nodeName : List.of("双面对裱", "覆双面", "双面喷")) {
            assertTrue(ProcedureFlowNodeMatcher.hasDoubleSideMountingNode(flowWithNode(nodeName)), nodeName);
            assertTrue(ProcedureFlowNodeMatcher.isDoubleSideMountingNodeName(nodeName), nodeName);
            assertTrue(ProcedureFlowNodeMatcher.containsDoubleSideMountingKeyword(nodeName), nodeName);
        }
    }

    @Test
    void shouldMatchOtherDoubleSideProcessAfterRemovingTapeTextFromCombinedValue() {
        assertTrue(ProcedureFlowNodeMatcher.containsDoubleSideMountingKeyword("覆双面胶、双面对裱"));
    }

    private ProcedureFlow flowWithNode(String nodeName) {
        ProcedureFlowNode node = new ProcedureFlowNode();
        node.setNodeName(nodeName);
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(List.of(node));
        return flow;
    }
}

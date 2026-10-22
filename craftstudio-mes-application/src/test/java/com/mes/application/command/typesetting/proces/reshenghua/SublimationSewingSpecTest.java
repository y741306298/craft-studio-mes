package com.mes.application.command.typesetting.proces.reshenghua;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SublimationSewingSpecTest {
    @ParameterizedTest
    @CsvSource({"上下缝,2", "左右缝,2", "上边缝,1", "下边缝,1", "左边缝,1", "右边缝,1"})
    void matchesEveryDirectionExactly(String node, int edgeCount) {
        assertEquals(edgeCount, SewingDirection.match(node).edges().size());
        assertNull(SewingDirection.match("其他" + node));
    }

    @Test
    void convertsCmAndKeepsIndependentDiametersForEachSewingProcess() {
        var specs = SublimationSewingSpec.parse(List.of("热升华", "缝管套", "其他工艺A", "白色直径2.5cm", "上下缝", "道旗缝边", "其他工艺B", "黑色直径1CM", "左边缝", "其他工艺", "右边缝"));
        assertEquals(2, specs.size());
        assertEquals("白色", specs.getFirst().color());
        var margins = SublimationSewingSpec.margins(specs);
        assertEquals(Math.PI * 25, margins.get(SewingDirection.Edge.TOP), 1e-8);
        assertEquals(Math.PI * 25, margins.get(SewingDirection.Edge.BOTTOM), 1e-8);
        assertEquals(Math.PI * 10, margins.get(SewingDirection.Edge.LEFT), 1e-8);
        assertFalse(margins.containsKey(SewingDirection.Edge.RIGHT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"缝管套", "道旗缝边"})
    void findsDiameterAfterInterveningNodes(String process) {
        var specs = SublimationSewingSpec.parse(Arrays.asList(process, "其他工艺", null, "", "白色直径2cm", "上下缝"));
        assertEquals(1, specs.size());
        assertEquals("白色", specs.getFirst().color());
        var margins = SublimationSewingSpec.margins(specs);
        assertEquals(Math.PI * 20, margins.get(SewingDirection.Edge.TOP), 1e-8);
        assertEquals(Math.PI * 20, margins.get(SewingDirection.Edge.BOTTOM), 1e-8);
    }

    @Test
    void repeatsDoNotAccumulateAndUnrelatedNodesDoNotTrigger() {
        var specs = SublimationSewingSpec.parse(List.of("缝管套", "红直径2cm", "上下缝", "上边缝"));
        assertEquals(Math.PI * 20, SublimationSewingSpec.margins(specs).get(SewingDirection.Edge.TOP), 1e-8);
        assertTrue(SublimationSewingSpec.parse(List.of("白色直径2cm", "上下缝")).isEmpty());
    }

    @Test
    void rejectsInvalidDiameterOrSeparatedDirections() {
        assertThrows(IllegalArgumentException.class, () -> SublimationSewingSpec.parse(List.of("道旗缝边", "白直径0cm", "上下缝")));
        assertThrows(IllegalArgumentException.class, () -> SublimationSewingSpec.parse(List.of("道旗缝边", "白直径2cm", "其他工艺", "上下缝")));
    }

    @Test
    void missingDiameterDoesNotRaiseAnAdjacencyValidationError() {
        assertTrue(SublimationSewingSpec.parse(List.of("道旗缝边", "其他工艺")).isEmpty());
    }
}

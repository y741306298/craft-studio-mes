package com.mes.application.command.delivery.packaging;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PackagingPatchTest {
    static Map<String, Object> m(Object... kv) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) result.put((String) kv[i], kv[i + 1]);
        return result;
    }
    static Map<String, Object> piece() {
        return m("quantity", 5, "status", "processing", "procedureFlow", m("nodes", List.of(
                m("nodeName", "其他工艺", "pieceQuantity", 0),
                m("nodeName", "待打包", "pieceQuantity", 5, "nodeStatus", "ACTIVE"),
                m("nodeName", "已打包", "pieceQuantity", 0, "nodeStatus", "PENDING"))));
    }
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> infos(Map<String, Object> patch) {
        return (List<Map<String, Object>>) patch.get("deliveryPkgInfos");
    }
    @Test void changesOnlyPackagingLeavesAndDoesNotMutateReadSnapshot() {
        var original = piece(); var before = piece();
        var patch = PackagingPatch.transfer(original, 2, 5, m("carrierId", "carrier"), "completed");
        assertEquals(3, patch.get("procedureFlow.nodes.1.pieceQuantity"));
        assertEquals(2, patch.get("procedureFlow.nodes.2.pieceQuantity"));
        assertFalse(patch.containsKey("status"));
        assertFalse(patch.containsKey("procedureFlow"));
        assertFalse(patch.containsKey("procedureFlow.nodes"));
        assertFalse(patch.containsKey("procedureFlow.nodes.0.pieceQuantity"));
        assertEquals(before, original);
        assertEquals(2, infos(patch).getFirst().get("quantity"));
    }
    @Test void completionPreservesFallbackAndConsolidationRules() {
        var patch = PackagingPatch.transfer(piece(), 5, null, m(), "completed");
        assertEquals("completed", patch.get("status"));
        assertEquals(0, patch.get("procedureFlow.nodes.1.pieceQuantity"));
        assertEquals(5, patch.get("procedureFlow.nodes.2.pieceQuantity"));
        assertFalse(patch.containsKey("procedureFlow.nodes.0.pieceQuantity"));
        assertFalse(PackagingPatch.transfer(piece(), 5, 6, m(), "completed").containsKey("status"));
    }
    @Test void rejectsOverPackingAndMissingNodesWithoutChangingSnapshot() {
        var piece = piece();
        assertThrows(IllegalStateException.class, () -> PackagingPatch.transfer(piece, 6, 5, m(), "completed"));
        assertThrows(IllegalStateException.class, () -> PackagingPatch.transfer(piece, 0, 5, m(), "completed"));
        assertThrows(IllegalStateException.class, () -> PackagingPatch.transfer(m(), 1, 1, m(), "completed"));
        assertEquals(5, PackagingPatch.nodes(piece).get(1).get("pieceQuantity"));
    }
    @Test void mergesExistingCarrierQuantityWithoutChangingOriginalPackageInfo() {
        var piece = piece();
        var existing = m("carrierId", "carrier", "kuaidiNum", "waybill", "quantity", 3);
        piece.put("deliveryPkgInfos", List.of(existing));
        var patch = PackagingPatch.transfer(piece, 2, 5,
                m("carrierId", "carrier", "kuaidiNum", "waybill", "carrierName", "送货上门"), "completed");
        assertEquals(5, infos(patch).getFirst().get("quantity"));
        assertEquals(3, existing.get("quantity"));
    }
}

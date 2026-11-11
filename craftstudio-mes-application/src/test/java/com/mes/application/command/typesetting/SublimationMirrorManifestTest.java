package com.mes.application.command.typesetting;

import com.mes.application.command.api.req.NestingRequest;
import com.mes.application.command.typesetting.strategy.policy.DoubleSideMountingManifestPolicy;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.MirrorConfig;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SublimationMirrorManifestTest {
    private final DoubleSideMountingManifestPolicy policy = new DoubleSideMountingManifestPolicy();

    @Test
    void mapsEachMarkedElementToItsOwnBackByMongoId() {
        ProductionPiece first = piece("one", "back-one.tif", "热升华", "反面相同画面");
        ProductionPiece second = piece("two", "back-two.tif", "热升华", "反面不同画面");
        ProductionPiece single = piece("single", "unused.tif", "热升华");
        List<ProductionPiece> pieces = List.of(first, second, single);
        NestingRequest.NestManifest manifest = new NestingRequest.NestManifest();
        manifest.setElements(List.of(element("marked-nesting-two"), element("one"), element("marked-nesting-single")));
        assertTrue(policy.matches(pieces, List.of()));
        policy.apply(manifest, pieces, List.of());
        assertEquals("back-two.tif", manifest.getElements().get(0).getMirrorConfig().get("two").getImg());
        assertEquals("back-one.tif", manifest.getElements().get(1).getMirrorConfig().get("one").getImg());
        assertNull(manifest.getElements().get(2).getMirrorConfig());
    }

    @Test
    void rejectsMissingBackRatherThanSubmittingFrontOnly() {
        ProductionPiece piece = piece("one", "", "热升华", "反面不同画面");
        NestingRequest.NestManifest manifest = new NestingRequest.NestManifest();
        manifest.setElements(List.of(element("marked-nesting-one")));
        assertThrows(IllegalArgumentException.class, () -> policy.apply(manifest, List.of(piece), List.of()));
    }

    @Test
    void existingDoubleSideMountingStillMapsBack() {
        ProductionPiece piece = piece("one", "back.tif", "双面对裱");
        NestingRequest.NestManifest manifest = new NestingRequest.NestManifest();
        manifest.setElements(List.of(element("one")));
        assertTrue(policy.matches(List.of(piece), List.of()));
        policy.apply(manifest, List.of(piece), List.of());
        assertEquals("back.tif", manifest.getElements().getFirst().getMirrorConfig().get("one").getImg());
    }

    private static NestingRequest.Element element(String id) {
        NestingRequest.Element element = new NestingRequest.Element();
        element.setId(id);
        return element;
    }

    static ProductionPiece piece(String id, String back, String... names) {
        ProductionPiece piece = new ProductionPiece();
        piece.setId(id);
        piece.setProcedureFlow(flow(names));
        MirrorConfig mirror = new MirrorConfig();
        mirror.setImg(back);
        piece.setMirrorConfigs(List.of(mirror));
        return piece;
    }

    static ProcedureFlow flow(String... names) {
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(Arrays.stream(names).map(name -> {
            ProcedureFlowNode node = new ProcedureFlowNode();
            node.setNodeName(name);
            return node;
        }).toList());
        return flow;
    }
}

package com.mes.application.command.typesetting;

import com.mes.application.command.api.req.NestingRequest;
import com.mes.application.command.orderPreprocessing.sublimation.SublimationSvg;
import com.mes.application.command.typesetting.strategy.policy.DoubleSideMountingManifestPolicy;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.MirrorConfig;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

import static org.junit.jupiter.api.Assertions.*;

class SublimationMirrorManifestTest {
    private final DoubleSideMountingManifestPolicy policy = new DoubleSideMountingManifestPolicy();

    @Test
    void mapsMarkedElementsToSvgGroupIdsAndPlainElementsToPieceIds() {
        ProductionPiece first = piece("one", "back-one.tif", "热升华", "反面相同画面");
        ProductionPiece second = piece("two", "back-two.tif", "热升华", "反面不同画面");
        ProductionPiece single = piece("single", "unused.tif", "热升华");
        List<ProductionPiece> pieces = List.of(first, second, single);
        NestingRequest.NestManifest manifest = new NestingRequest.NestManifest();
        manifest.setElements(List.of(element("marked-nesting-two"), element("one"), element("marked-nesting-single")));
        assertTrue(policy.matches(pieces, List.of()));
        policy.apply(manifest, pieces, List.of());
        assertEquals("back-two.tif", manifest.getElements().get(0).getMirrorConfig().get("sublimation-original-two").getImg());
        assertFalse(manifest.getElements().get(0).getMirrorConfig().containsKey("two"));
        assertEquals("back-one.tif", manifest.getElements().get(1).getMirrorConfig().get("one").getImg());
        assertNull(manifest.getElements().get(2).getMirrorConfig());
    }

    @Test
    void reportedMarkedElementTargetsTheActualImageGroupInStoredSvg() throws Exception {
        String id = "6af4818cc8b810d8ca646365";
        ProductionPiece piece = piece(id, "back-expanded.tif", "热升华", "反面相同画面");
        NestingRequest.Element element = element("marked-nesting-" + id);
        NestingRequest.NestManifest manifest = new NestingRequest.NestManifest();
        manifest.setElements(List.of(element));
        policy.apply(manifest, List.of(piece), List.of());

        // 使用已经存储的 SVG ID 格式，而非只比较请求对象中的字符串。
        String svg = SublimationSvg.open(1000, 2000)
                + SublimationSvg.original(id, "front-expanded.tif", 1000, 2000)
                + SublimationSvg.border(id, "front-border.png", 1000, 2000) + "</svg>";
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
        String key = element.getMirrorConfig().keySet().iterator().next();
        int matches = 0;
        var groups = document.getElementsByTagName("g");
        for (int i = 0; i < groups.getLength(); i++) {
            Element group = (Element) groups.item(i);
            if (key.equals(group.getAttribute("id"))) {
                assertEquals("front-expanded.tif", group.getAttribute("img"));
                matches++;
            }
        }
        assertEquals(1, matches, "mirrorConfig key 必须唯一命中实际画面分组");
        assertEquals("back-expanded.tif", element.getMirrorConfig().get(key).getImg());
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

    @Test
    void markedOrdinaryDoubleSideMountingKeepsItsExistingInternalId() {
        ProductionPiece piece = piece("one", "back.tif", "双面对裱");
        NestingRequest.NestManifest manifest = new NestingRequest.NestManifest();
        manifest.setElements(List.of(element("marked-nesting-one")));
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

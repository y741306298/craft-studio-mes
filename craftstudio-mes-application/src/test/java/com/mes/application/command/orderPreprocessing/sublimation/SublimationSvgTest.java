package com.mes.application.command.orderPreprocessing.sublimation;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SublimationSvgTest {
    @Test
    void nestingMirrorKeyFindsExistingSublimationImageGroup() throws Exception {
        String pieceId = "6af4818cc8b810d8ca646365";
        String svg = SublimationSvg.open(1044, 2074)
                + SublimationSvg.original(pieceId, "front.tif", 1044, 2074)
                + SublimationSvg.border(pieceId, "border.png", 1044, 2074) + "</svg>";
        Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8))).getDocumentElement();
        var groups = root.getElementsByTagName("g");
        String key = SublimationSvg.originalGroupId(pieceId);
        assertEquals("sublimation-original-" + pieceId, key);
        int targets = 0;
        for (int i = 0; i < groups.getLength(); i++) {
            Element group = (Element) groups.item(i);
            assertNotEquals(pieceId, group.getAttribute("id"), "旧的裸ID不是SVG内部画面分组ID");
            if (key.equals(group.getAttribute("id"))) {
                assertEquals("front.tif", group.getAttribute("img"));
                targets++;
            }
        }
        assertEquals(1, targets);
    }

    @Test
    void backSvgContainsOnlyExpandedBackImageWithoutMarks() throws Exception {
        String url = "https://example.test/back.tif?a=1&b=2";
        String svg = SublimationSvg.forBack("piece", url, 1044, 2074);
        Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8))).getDocumentElement();
        assertEquals("1044", root.getAttribute("width"));
        assertEquals("2074", root.getAttribute("height"));
        assertEquals("0 0 1044 2074", root.getAttribute("viewBox"));
        assertEquals(1, root.getElementsByTagName("g").getLength());
        Element image = (Element) root.getElementsByTagName("g").item(0);
        assertEquals(url, image.getAttribute("img"));
        assertEquals("true", image.getAttribute("require-plt"));
        assertEquals(1, image.getElementsByTagName("path").getLength());
        assertEquals("M0 0 H1044 V2074 H0 Z", ((Element) image.getFirstChild()).getAttribute("d"));
        assertFalse(svg.contains("sublimation-border"));
        assertFalse(svg.contains("sublimation-tag"));
        assertFalse(svg.contains("sublimation-buckle"));
        assertFalse(svg.contains("sublimation-tie"));
    }
}

package com.mes.application.command.orderPreprocessing.sublimation;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SublimationSvgTest {
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

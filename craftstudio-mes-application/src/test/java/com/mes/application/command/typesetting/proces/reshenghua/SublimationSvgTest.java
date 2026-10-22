package com.mes.application.command.typesetting.proces.reshenghua;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SublimationSvgTest {
    @ParameterizedTest
    @CsvSource({"上下缝,0,0,1,1", "左右缝,1,1,0,0", "上边缝,0,0,1,0", "下边缝,0,0,0,1", "左边缝,1,0,0,0", "右边缝,0,1,0,0"})
    void expandsOnlySelectedEdges(String node, int left, int right, int top, int bottom) {
        var svg = svg("<svg xmlns='http://www.w3.org/2000/svg' width='500' height='500'><rect x='20' y='30' width='100' height='80'/></svg>");
        var margins = SublimationSewingSpec.margins(SublimationSewingSpec.parse(List.of("道旗缝边", "黑色直径1.5cm", node)));
        var b = svg.expanded(svg.extension(margins)).getBounds2D();
        double distance = Math.PI * 15;
        assertEquals(20 - left * distance, b.getMinX(), 1e-8);
        assertEquals(120 + right * distance, b.getMaxX(), 1e-8);
        assertEquals(30 - top * distance, b.getMinY(), 1e-8);
        assertEquals(110 + bottom * distance, b.getMaxY(), 1e-8);
    }

    @ParameterizedTest
    @CsvSource({"mm,100,100", "cm,10,100", "px,960,254", "in,10,254", "pt,720,254"})
    void convertsPhysicalRootUnits(String unit, double width, double expectedMm) {
        var svg = svg("<svg xmlns='http://www.w3.org/2000/svg' width='" + width + unit + "' height='" + width + unit + "' viewBox='0 0 100 100'><rect width='100' height='100'/></svg>");
        assertEquals(expectedMm, svg.bounds().getWidth(), 1e-6);
    }

    @Test
    void expandsFromShapeExtremaWithOffsetViewBoxAndNestedTransforms() {
        var svg = svg("<svg xmlns='http://www.w3.org/2000/svg' width='200mm' height='100mm' viewBox='-50 10 1000 500'><g transform='translate(100 50)'><g transform='scale(2)'><rect x='10' y='20' width='200' height='100'/></g></g></svg>");
        Rectangle2D original = svg.bounds();
        assertEquals(34, original.getMinX(), 1e-8);
        assertEquals(16, original.getMinY(), 1e-8);
        assertEquals(80, original.getWidth(), 1e-8);
        var margins = SublimationSewingSpec.margins(SublimationSewingSpec.parse(List.of("缝管套", "白直径2cm", "左边缝")));
        Area strips = svg.extension(margins);
        Rectangle2D expanded = svg.expanded(strips).getBounds2D();
        assertEquals(34 - Math.PI * 20, expanded.getMinX(), 1e-6);
        assertEquals(original.getMaxX(), expanded.getMaxX(), 1e-6);
        assertEquals(original.getMinY(), expanded.getMinY(), 1e-6);
        assertFalse(strips.contains(original.getCenterX(), original.getCenterY()));
    }

    @Test
    void supportsCurveAndArcExtremaInsteadOfControlPointBounds() {
        var svg = svg("<svg xmlns='http://www.w3.org/2000/svg' width='400' height='400'><path d='M10 20 C10 -100 110 -100 110 20 L110 120 L10 120 Z'/></svg>");
        assertEquals(-70, svg.bounds().getMinY(), 1e-6);
        assertEquals(120, svg.bounds().getMaxY(), 1e-6);
        var circle = svg("<svg xmlns='http://www.w3.org/2000/svg' width='400' height='400'><path d='M100 50 A50 50 0 1 1 0 50 A50 50 0 1 1 100 50 Z'/></svg>");
        assertEquals(100, circle.bounds().getWidth(), 1e-5);
        assertEquals(100, circle.bounds().getHeight(), 1e-5);
    }

    @Test
    void excludesMarksAndUnusedDefinitionsAndResolvesLocalUse() {
        var svg = svg("<svg xmlns='http://www.w3.org/2000/svg' width='500' height='500'><defs><rect id='shape' width='100' height='50'/><rect width='9999' height='9999'/></defs><use href='#shape' x='20' y='30'/><g data-forme='false'><rect x='-999' y='-999' width='2000' height='2000'/></g></svg>");
        assertEquals(20, svg.bounds().getMinX(), 1e-8);
        assertEquals(30, svg.bounds().getMinY(), 1e-8);
        assertEquals(100, svg.bounds().getWidth(), 1e-8);
    }

    @Test
    void outlinePreservesNonRectangularContourAndOmitsHoles() {
        var svg = svg("<svg xmlns='http://www.w3.org/2000/svg' width='200' height='200'><path fill-rule='evenodd' d='M0 0 H100 V100 H0 Z M20 20 H80 V80 H20 Z'/></svg>");
        assertTrue(SublimationSvg.exterior(svg.expanded(new Area())).contains(50, 50));
        var triangle = svg("<svg xmlns='http://www.w3.org/2000/svg' width='200' height='200'><path d='M0 0 L100 50 L0 100 Z'/></svg>");
        Area shape = SublimationSvg.exterior(triangle.expanded(triangle.extension(Map.of(SewingDirection.Edge.LEFT, 20D))));
        assertTrue(shape.contains(-10, 50));
        assertFalse(shape.contains(95, 95));
    }

    @Test
    void rewritesInMmKeepsGroupsAndAppendsTwoMmStrokeLast() throws Exception {
        var svg = svg("<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100'><g id='existing' img='source.png'><rect x='10' y='20' width='50' height='40'/></g></svg>");
        Area strips = svg.extension(Map.of(SewingDirection.Edge.TOP, Math.PI * 10));
        Area outline = SublimationSvg.exterior(svg.expanded(strips));
        String rewritten = svg.rewrite("piece", "source.png", strips, outline, "border.png", "<g id='flow-tag' data-forme='false'><text>流程 &amp; mark</text></g>");
        var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(rewritten)));
        Element root = doc.getDocumentElement();
        Element last = (Element) root.getLastChild();
        assertEquals("reshenghua-outline-piece", last.getAttribute("id"));
        assertEquals("2", ((Element) last.getFirstChild()).getAttribute("stroke-width"));
        assertEquals("none", ((Element) last.getFirstChild()).getAttribute("fill"));
        assertTrue(rewritten.contains("id=\"existing\""));
        assertTrue(rewritten.contains("img=\"source.png\""));
        assertTrue(root.getAttribute("width").endsWith("mm"));
        assertEquals(40 + Math.PI * 10, Double.parseDouble(root.getAttribute("height").replace("mm", "")), 1e-6);
    }

    @Test
    void honorsDefaultAndNoneAspectRatio() {
        var defaultAspect = svg("<svg xmlns='http://www.w3.org/2000/svg' width='200mm' height='100mm' viewBox='0 0 100 100'><rect width='100' height='100'/></svg>");
        assertEquals(50, defaultAspect.bounds().getMinX(), 1e-8);
        assertEquals(100, defaultAspect.bounds().getWidth(), 1e-8);
        var none = svg("<svg xmlns='http://www.w3.org/2000/svg' width='200mm' height='100mm' viewBox='0 0 100 100' preserveAspectRatio='none'><rect width='100' height='100'/></svg>");
        assertEquals(200, none.bounds().getWidth(), 1e-8);
    }

    @Test
    void rejectsExternalEntitiesAndClippingInsteadOfProducingWrongDimensions() {
        assertThrows(IllegalArgumentException.class, () -> svg("<!DOCTYPE svg [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><svg xmlns='http://www.w3.org/2000/svg'>&x;</svg>"));
        assertThrows(IllegalArgumentException.class, () -> svg("<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100'><path clip-path='url(#clip)' d='M0 0H100V100H0Z'/></svg>"));
    }

    private SublimationSvg svg(String svg) { return new SublimationSvg(svg, 100D, 100D); }
}

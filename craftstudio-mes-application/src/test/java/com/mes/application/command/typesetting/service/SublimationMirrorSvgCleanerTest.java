package com.mes.application.command.typesetting.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SublimationMirrorSvgCleanerTest {
    @Test
    void removesAllSublimationMarksAndKeepsImageAndPlacement() {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="1200" height="2400">
                  <g id="marked-nesting-piece" transform="translate(30 40) rotate(90)">
                    <g id="sublimation-original-piece" img="back-expanded.tif"><path d="M0 0 H100 V200 H0 Z"/></g>
                    <g transform="translate(2 2)"><g id="sublimation-tag-top-piece"><g><path/></g></g></g>
                    <g id="sublimation-border-piece"><path/></g>
                    <g id="sublimation-buckle-corner-piece"><path/></g>
                    <g id="sublimation-tie-left-piece"><path/></g>
                    <g id="sublimation-trace-code-piece-1-2-3" data-trace-code="1-2-3">
                      <g><g id="sublimation-trace-top-piece"><path/></g></g>
                    </g>
                  </g>
                </svg>
                """;
        String clean = SublimationMirrorSvgCleaner.clean(svg);
        assertTrue(clean.contains("sublimation-original-piece"));
        assertTrue(clean.contains("back-expanded.tif"));
        assertTrue(clean.contains("translate(30 40) rotate(90)"));
        assertTrue(clean.contains("M0 0 H100 V200 H0 Z"));
        assertFalse(clean.contains("sublimation-tag"));
        assertFalse(clean.contains("sublimation-border"));
        assertFalse(clean.contains("sublimation-buckle"));
        assertFalse(clean.contains("sublimation-tie"));
        assertFalse(clean.contains("sublimation-trace"));
    }

    @Test
    void preservesAlreadyCleanSvgExactly() {
        String svg = "<svg><g id='sublimation-original-piece' img='back.tif'><path/></g></svg>";
        assertEquals(svg, SublimationMirrorSvgCleaner.clean(svg));
    }
}

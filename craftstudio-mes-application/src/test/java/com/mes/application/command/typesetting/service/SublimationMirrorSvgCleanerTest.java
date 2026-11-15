package com.mes.application.command.typesetting.service;

class SublimationMirrorSvgCleanerTest {
    @org.junit.jupiter.api.Test void delegatesCleaningToRemoteService() {
        try (var rpc = org.mockito.Mockito.mockStatic(com.mes.application.command.graphics.RemoteGraphics.class)) {
            rpc.when(() -> com.mes.application.command.graphics.RemoteGraphics.svg(org.mockito.ArgumentMatchers.eq("SUB_CLEAN"), org.mockito.ArgumentMatchers.anyMap())).thenReturn("clean-svg");
            org.junit.jupiter.api.Assertions.assertEquals("clean-svg", SublimationMirrorSvgCleaner.clean("source-svg"));
        }
    }
}

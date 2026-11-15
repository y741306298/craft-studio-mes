package com.mes.application.command.orderPreprocessing.sublimation;

class SublimationSvgTest {
    @org.junit.jupiter.api.Test void delegatesBackSvgToRemoteService() {
        try (var rpc = org.mockito.Mockito.mockStatic(com.mes.application.command.graphics.RemoteGraphics.class)) {
            rpc.when(() -> com.mes.application.command.graphics.RemoteGraphics.svg(org.mockito.ArgumentMatchers.eq("SUB_BACK"), org.mockito.ArgumentMatchers.anyMap())).thenReturn("remote-svg");
            org.junit.jupiter.api.Assertions.assertEquals("remote-svg", SublimationSvg.forBack("piece", "back.png", 100, 200));
        }
    }
}

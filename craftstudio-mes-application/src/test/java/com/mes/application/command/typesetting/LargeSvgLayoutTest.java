package com.mes.application.command.typesetting;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.graphics.ImgUtilClient;
import com.mes.application.command.graphics.RemoteGraphics;
import com.mes.application.command.graphics.RemoteImage;
import com.mes.application.command.graphics.RemotePng;
import com.mes.application.command.graphics.SvgResourceReader;
import com.mes.application.command.typesetting.layout.FormeBuildContext;
import com.mes.application.command.typesetting.layout.LiuduLayoutBuildService;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.application.command.typesetting.vo.TypesettingProductionPieceVO;
import com.mes.application.dto.req.typesetting.LayoutConfirmRequest;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LargeSvgLayoutTest {

    private static final String LARGE_SVG = """
            <!DOCTYPE svg>
            <svg xmlns="http://www.w3.org/2000/svg" width="1000mm" viewBox="0, 0, 1000, 950">
              <desc>%s</desc>
              <g data-forme="true" id="marked-nesting-piece" transform="matrix(1 0 0 1 30 40)">
                <g id='piece' transform="matrix(1 0 0 1 12 15)">
                  <rect x="0" y="0" width="70" height="650"/>
                </g>
              </g>
              <!-- id="other" -->
              <g id='other'/>
            </svg>
            """.formatted("图".repeat(400_000));

    private HttpServer server;
    private String url;

    @BeforeEach
    void serveChunkedSvg() throws IOException {
        byte[] svg = LARGE_SVG.getBytes(StandardCharsets.UTF_8);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/nested.svg", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (var body = exchange.getResponseBody()) {
                body.write(svg);
            }
        });
        server.start();
        url = "http://127.0.0.1:" + server.getAddress().getPort() + "/nested.svg";
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void readsOriginalDimensionsAndSourceIdsFromLargeSvg() {
        AppTypesettingService service = new AppTypesettingService();
        Object size = ReflectionTestUtils.invokeMethod(service, "resolveRawNestedSize", layout());
        assertNotNull(size);
        assertEquals(BigDecimal.valueOf(1000), ReflectionTestUtils.getField(size, "width"));
        assertEquals(BigDecimal.valueOf(950), ReflectionTestUtils.getField(size, "height"));

        LayoutConfirmRequest request = new LayoutConfirmRequest();
        request.setTypesettingCells(List.of(source("piece"), source("other")));
        List<TypesettingSourceCell> cells = ReflectionTestUtils.invokeMethod(service, "extractUsedSourceCells", request, url);
        assertEquals(List.of("piece", "other"), cells.stream().map(TypesettingSourceCell::getSourceId).toList());
        assertEquals(List.of(1, 1), cells.stream().map(TypesettingSourceCell::getQuantity).toList());
    }

    @Test
    void placesLiuduMarksFromTransformedGeometryInLargeSvg() {
        ImgUtilClient renderer = mock(ImgUtilClient.class);
        when(renderer.render(eq("workflow"), eq("BLACK"), anyMap()))
                .thenReturn(new RemoteImage("task", "https://assets.example/mark.png", 10, 10));
        OssTagUploadService uploads = mock(OssTagUploadService.class);
        when(uploads.uploadLagPng(eq("layout"), any(RemotePng.class)))
                .thenAnswer(call -> call.getArgument(1, RemotePng.class).url());

        RemoteGraphics.enter(renderer, "workflow");
        try {
            FormeBuildContext context = new FormeBuildContext();
            context.setTypesettingInfo(layout());
            context.setBusinessId("layout");
            context.setNestedWidth(BigDecimal.valueOf(1000));
            context.setNestedHeight(BigDecimal.valueOf(950));
            List<FormeGenerationRequest.Position> positions = new LiuduLayoutBuildService(uploads).build(context)
                    .getMarks().stream().map(FormeGenerationRequest.Mark::getPosition).toList();
            assertEquals(List.of(8, 1023, 37, 43, 43, 1053, 1053),
                    positions.stream().map(FormeGenerationRequest.Position::getX).toList());
            assertEquals(List.of(0, 0, 225, 135, 155, 135, 155),
                    positions.stream().map(FormeGenerationRequest.Position::getY).toList());
        } finally {
            RemoteGraphics.exit();
        }
    }

    @Test
    void readsCompleteSvgTextForPieceEditing() {
        assertEquals(LARGE_SVG, SvgResourceReader.text(url));
    }

    private TypesettingInfo layout() {
        TypesettingElement element = new TypesettingElement();
        element.setNestedSvg(url);
        element.setWidth(BigDecimal.valueOf(1040));
        element.setHeight(BigDecimal.valueOf(990));
        TypesettingInfo info = new TypesettingInfo();
        info.setElement(element);
        return info;
    }

    private TypesettingProductionPieceVO source(String id) {
        TypesettingProductionPieceVO piece = new TypesettingProductionPieceVO();
        piece.setSourceType("PART");
        piece.setSourceId(id);
        return piece;
    }
}

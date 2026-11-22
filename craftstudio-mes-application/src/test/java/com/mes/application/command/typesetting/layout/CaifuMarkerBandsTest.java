package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.graphics.ImgUtilClient;
import com.mes.application.command.graphics.RemoteGraphics;
import com.mes.application.command.graphics.RemoteImage;
import com.mes.application.command.graphics.RemotePng;
import com.mes.application.command.graphics.SvgResourceReader;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaifuMarkerBandsTest {

    private static final String SEPARATOR_ID = "ac30f000-0000-4000-a000-000000000001";
    private static final String ELEMENT_B = "https://assets.example/elementB.png";
    private static final String ELEMENT_D = "https://assets.example/elementD.png";
    private static final int NESTED_HEIGHT_MM = 66897;
    private static final List<Integer> SEPARATOR_YS = List.of(2337, 4680, 7023, 9366, 10611, 11976,
            13810, 16276, 17921, 18667, 21014, 23261, 25704, 26810, 29251, 29635, 30447, 32481,
            34926, 36512, 38481, 40523, 41263, 42974, 43786, 45792, 47775, 49753, 51731, 52377,
            54670, 56622, 58888, 60921, 62661, 64712, 65760);

    @Test
    void readsLargeNestedSvgAndPlacesBAndDForBothA30HModes() throws Exception {
        byte[] svg = nestedSvg().getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/nested.svg", exchange -> {
            exchange.sendResponseHeaders(200, svg.length);
            try (var body = exchange.getResponseBody()) {
                body.write(svg);
            }
        });
        server.start();

        ImgUtilClient renderer = mock(ImgUtilClient.class);
        when(renderer.render(eq("workflow"), anyString(), anyMap())).thenAnswer(call -> {
            Map<String, Object> params = call.getArgument(2);
            Number width = (Number) params.get("width");
            Number height = (Number) params.get("height");
            String url = width == null ? "https://assets.example/tag.png"
                    : width.doubleValue() == 8 ? ELEMENT_B
                    : width.doubleValue() == 0.3 && height.intValue() == 5 ? ELEMENT_D : "https://assets.example/other.png";
            return new RemoteImage("task", url, 8, 5);
        });
        OssTagUploadService uploads = mock(OssTagUploadService.class);
        when(uploads.uploadTagPng(anyString(), any(RemotePng.class), anyString()))
                .thenAnswer(call -> call.getArgument(1, RemotePng.class).url());
        TypesettingService layouts = mock(TypesettingService.class);
        TypesettingInfo bloodPlate = new TypesettingInfo();
        bloodPlate.setHaveBlood(true);
        when(layouts.findById("plate-0")).thenReturn(bloodPlate);
        when(layouts.findById("plate-1")).thenReturn(bloodPlate);

        RemoteGraphics.enter(renderer, "workflow");
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/nested.svg";
            assertThrows(IllegalArgumentException.class, () -> SvgResourceReader.read(url));
            var noFilm = new CaifuOpenBackA30HNoFilmLayoutBuildService(uploads, layouts).build(context(url));
            var film = new CaifuOpenBackA30HFilmLayoutBuildService(uploads, layouts).build(context(url));
            assertEquals(38, positions(noFilm, ELEMENT_B).size());
            assertEquals(38, positions(film, ELEMENT_B).size());
            assertTrue(positions(noFilm, ELEMENT_B).containsAll(List.of(2065, 66625)));
            assertTrue(positions(film, ELEMENT_B).containsAll(List.of(315, 2655)));
            assertEquals(List.of(28, 28, 2344, 2344, 2368, 2368, 4684, 4684), positions(noFilm, ELEMENT_D));
            assertEquals(List.of(28, 28, 2368, 2368), positions(film, ELEMENT_D));
            assertEquals(List.of(1016, 1026), noFilm.getMarks().stream().filter(mark -> ELEMENT_D.equals(mark.getImg()))
                    .map(FormeGenerationRequest.Mark::getPosition).map(FormeGenerationRequest.Position::getX)
                    .distinct().sorted().toList());
        } finally {
            RemoteGraphics.exit();
            server.stop(0);
        }
    }

    private List<Integer> positions(FormeLayoutBuildResult result, String image) {
        return result.getMarks().stream().filter(mark -> image.equals(mark.getImg()))
                .map(FormeGenerationRequest.Mark::getPosition).map(FormeGenerationRequest.Position::getY).toList();
    }

    private FormeBuildContext context(String url) {
        List<TypesettingSourceCell> cells = new ArrayList<>();
        for (int index = 0; index <= SEPARATOR_YS.size(); index++) {
            TypesettingSourceCell cell = new TypesettingSourceCell();
            cell.setSourceType("TYPESETTING");
            cell.setSourceId("plate-" + index);
            cells.addFirst(cell);
        }
        TypesettingElement element = new TypesettingElement();
        element.setNestedSvg(url);
        TypesettingInfo typesetting = new TypesettingInfo();
        typesetting.setElement(element);
        typesetting.setTypesettingCells(cells);
        typesetting.setMarks(Map.of("caifuMarker_" + SEPARATOR_ID, "https://assets.example/imgutil/result.png"));
        FormeBuildContext context = new FormeBuildContext();
        context.setTypesettingInfo(typesetting);
        context.setBusinessId("layout");
        context.setNestedWidth(BigDecimal.valueOf(1035));
        context.setNestedHeight(BigDecimal.valueOf(NESTED_HEIGHT_MM));
        return context;
    }

    private String nestedSvg() {
        StringBuilder svg = new StringBuilder("<svg xmlns='http://www.w3.org/2000/svg'><desc>");
        svg.append("a".repeat(1_100_000)).append("</desc>");
        int top = 0;
        for (int index = 0; index <= SEPARATOR_YS.size(); index++) {
            int bottom = index < SEPARATOR_YS.size() ? SEPARATOR_YS.get(index) : NESTED_HEIGHT_MM;
            svg.append("<g id='plate-").append(index).append("' data-cell-height='").append(bottom - top).append("'/>");
            if (index < SEPARATOR_YS.size()) {
                svg.append("<g id='").append(SEPARATOR_ID).append("' height='6' transform='translate(0,")
                        .append(bottom).append(")'/>");
                top = bottom + 6;
            }
        }
        return svg.append("</svg>").toString();
    }
}

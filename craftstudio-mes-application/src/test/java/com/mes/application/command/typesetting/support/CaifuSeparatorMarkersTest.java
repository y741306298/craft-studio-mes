package com.mes.application.command.typesetting.support;

import com.alibaba.fastjson2.JSON;
import com.mes.application.command.api.req.NestingRequest;
import com.mes.application.command.graphics.ImgUtilClient;
import com.mes.application.command.graphics.RemoteGraphics;
import com.mes.application.command.graphics.RemoteImage;
import com.mes.application.command.typesetting.nesting.CaifuOpenBackA30HFilmNestingComposeService;
import com.mes.application.command.typesetting.nesting.CaifuOpenBackA30HNoFilmNestingComposeService;
import com.mes.application.command.typesetting.nesting.CaifuOpenBackA30ProNoFilmNestingComposeService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaifuSeparatorMarkersTest {

    @Test
    void recordsOnePngPerSeparatorIdWhenImgUtilNamesBothFilesIndependently() {
        String png = "https://assets.example/imgutil/render-png/result.png";
        String svg = "https://assets.example/imgutil/save-svg/result.svg";
        ImgUtilClient imgUtil = mock(ImgUtilClient.class);
        OssTagUploadService uploads = mock(OssTagUploadService.class);
        when(imgUtil.render(eq("workflow"), eq("NESTING_MARKER"), anyMap()))
                .thenReturn(new RemoteImage("render-png", png, 1500, 6));
        when(imgUtil.svg(eq("workflow"), eq("NESTING_MARKER_SVG"), anyMap())).thenReturn("<svg/>");
        when(uploads.uploadTagSvg(eq("job"), any(byte[].class), eq("mark/factory/caifu"), anyString()))
                .thenReturn(svg);
        var modes = List.of(new CaifuOpenBackA30HFilmNestingComposeService(uploads),
                new CaifuOpenBackA30HNoFilmNestingComposeService(uploads),
                new CaifuOpenBackA30ProNoFilmNestingComposeService(uploads));
        NestingRequest.Element piece = new NestingRequest.Element();
        piece.setId("piece");
        piece.setImg(png);

        RemoteGraphics.enter(imgUtil, "workflow");
        try {
            for (var mode : modes) {
                var elements = mode.composeElements("factory", "job", List.of(piece, piece, piece), List.of());
                assertEquals(5, elements.size());
                NestingRequest.Element marker = elements.get(1);
                assertEquals(marker.getId(), elements.get(3).getId());
                assertEquals(svg, marker.getSvg());
                assertEquals(png, marker.getImg());
                assertEquals(JSON.parseObject(JSON.toJSONString(newPlainElement(marker))),
                        JSON.parseObject(JSON.toJSONString(marker)));

                NestingRequest.NestManifest manifest = new NestingRequest.NestManifest();
                manifest.setElements(elements);
                NestingRequest request = new NestingRequest();
                request.setNestManifest(manifest);
                Map<String, String> marks = CaifuSeparatorMarkers.collect(request);
                assertEquals(Map.of("caifuMarker_" + marker.getId(), png), marks);
                assertEquals(Set.of(marker.getId()), CaifuSeparatorMarkers.groupIds(marks));
            }
        } finally {
            RemoteGraphics.exit();
        }
    }

    @Test
    void recoversGroupIdFromThePngOfAnOlderNumberedMarker() {
        String id = "d14a0630-292c-41db-831c-27e9ffc8a51c";
        assertEquals(Set.of(id), CaifuSeparatorMarkers.groupIds(Map.of(
                "caifuMarker_1", "https://assets.example/mark/factory/caifu/" + id + ".png?download=1",
                "elementD", "https://assets.example/elementD.png")));
    }

    private NestingRequest.Element newPlainElement(NestingRequest.Element marker) {
        NestingRequest.Element element = new NestingRequest.Element();
        element.setId(marker.getId());
        element.setImg(marker.getImg());
        element.setSvg(marker.getSvg());
        element.setCounts(1);
        element.setForme(false);
        element.setHGravity("left");
        element.setHMargin(0);
        element.setVMargin(0);
        return element;
    }
}

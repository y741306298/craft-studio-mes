package com.mes.application.command.typesetting.nesting;

import com.mes.application.command.api.req.NestingRequest;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class CaifuOpenBackA30HFilmNestingComposeService implements NestingRequestComposeService {

    private final OssTagUploadService ossTagUploadService;

    public CaifuOpenBackA30HFilmNestingComposeService(OssTagUploadService ossTagUploadService) {
        this.ossTagUploadService = ossTagUploadService;
    }

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30H_FILM;
    }

    @Override
    public int resolveSpacing(TypesettingLayoutMode layoutMode) {
        return 0;
    }

    @Override
    public List<NestingRequest.Element> composeElements(String manufacturerMetaId,
                                                        String businessId,
                                                        List<NestingRequest.Element> elements,
                                                        List<NestingRequest.Container> containers) {
        if (elements == null || elements.size() < 2) {
            return elements;
        }
        String markerId = java.util.UUID.randomUUID().toString();
        MarkerAsset markerAsset = uploadCaifuOpenBackMarker(manufacturerMetaId, businessId, containers, markerId);
        List<NestingRequest.Element> result = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            result.add(elements.get(i));
            if (i < elements.size() - 1) {
                NestingRequest.Element markerElement = new NestingRequest.Element();
                markerElement.setId(markerId);
                markerElement.setImg(markerAsset.pngUrl);
                markerElement.setSvg(markerAsset.svgUrl);
                markerElement.setCounts(1);
                markerElement.setForme(Boolean.FALSE);
                markerElement.setHGravity("left");
                markerElement.setHMargin(0);
                markerElement.setVMargin(0);
                result.add(markerElement);
            }
        }
        return result;
    }

    private MarkerAsset uploadCaifuOpenBackMarker(String manufacturerMetaId, String businessId, List<NestingRequest.Container> containers, String markerId) {
        int width = containers.stream()
                .filter(Objects::nonNull)
                .map(NestingRequest.Container::getWidth)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(1500);

        var png = com.mes.application.command.graphics.RemoteGraphics.png("NESTING_MARKER", com.mes.application.command.graphics.RemoteGraphics.params("width", width, "height", 6, "left", markerLineLeftMm()));
        String svg = buildMarkerSvg(width);
        String url = ossTagUploadService.uploadTagSvg(businessId, svg.getBytes(StandardCharsets.UTF_8), "mark/" + manufacturerMetaId + "/caifu", markerId + ".svg");
        return new MarkerAsset(png.url(), url);
    }

    protected int markerLineLeftMm() {
        return 0;
    }

    private int markerLineWidthMm(int width) {
        // 保留原有的右侧 30mm 留白，左端内缩时同步缩短黑线。
        return Math.max(width - 30 - markerLineLeftMm(), 1);
    }

    private String buildMarkerSvg(int width) {
        return com.mes.application.command.graphics.RemoteGraphics.svg("NESTING_MARKER_SVG", com.mes.application.command.graphics.RemoteGraphics.params("width", width, "left", markerLineLeftMm()));
    }

    private static class MarkerAsset {
        private final String pngUrl;
        private final String svgUrl;

        private MarkerAsset(String pngUrl, String svgUrl) {
            this.pngUrl = pngUrl;
            this.svgUrl = svgUrl;
        }
    }
}

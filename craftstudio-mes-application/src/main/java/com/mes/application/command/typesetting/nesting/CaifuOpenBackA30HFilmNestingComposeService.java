package com.mes.application.command.typesetting.nesting;

import com.mes.application.command.api.req.NestingRequest;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
        BufferedImage image = new BufferedImage(width, 6, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, 6);
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 2, Math.max(width - 30, 1), 1);
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", outputStream);
            String subDir = "mark/" + manufacturerMetaId + "/caifu";
            String pngUrl = ossTagUploadService.uploadTagPng(businessId, outputStream.toByteArray(), subDir, markerId + ".png");
            String svg = buildMarkerSvg(width);
            String svgUrl = ossTagUploadService.uploadTagSvg(businessId, svg.getBytes(StandardCharsets.UTF_8), subDir, markerId + ".svg");
            return new MarkerAsset(pngUrl, svgUrl);
        } catch (Exception e) {
            throw new IllegalStateException("上传裁赋开背辅助标记失败", e);
        }
    }

    private String buildMarkerSvg(int width) {
        int lineWidth = Math.max(width - 30, 1);
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + width + "mm\" height=\"6mm\" viewBox=\"0 0 " + width + " 6\">"
                + "<rect x=\"0\" y=\"0\" width=\"" + width + "\" height=\"6\" fill=\"#ffffff\"/>"
                + "<rect x=\"0\" y=\"2.5\" width=\"" + lineWidth + "\" height=\"1\" fill=\"#000000\"/>"
                + "</svg>";
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

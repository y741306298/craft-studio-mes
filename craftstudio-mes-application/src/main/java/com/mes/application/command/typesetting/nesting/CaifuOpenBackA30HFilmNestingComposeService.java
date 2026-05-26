package com.mes.application.command.typesetting.nesting;

import com.mes.application.command.api.req.NestingRequest;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
        String markerImgUrl = uploadCaifuOpenBackMarker(manufacturerMetaId, businessId, containers);
        List<NestingRequest.Element> result = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            result.add(elements.get(i));
            if (i < elements.size() - 1) {
                NestingRequest.Element markerElement = new NestingRequest.Element();
                markerElement.setId("caifu-open-back-marker-" + i);
                markerElement.setImg(markerImgUrl);
                markerElement.setSvg(markerImgUrl);
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

    private String uploadCaifuOpenBackMarker(String manufacturerMetaId, String businessId, List<NestingRequest.Container> containers) {
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
            return ossTagUploadService.uploadTagPng(businessId, outputStream.toByteArray(), subDir);
        } catch (Exception e) {
            throw new IllegalStateException("上传裁赋开背辅助标记失败", e);
        }
    }
}

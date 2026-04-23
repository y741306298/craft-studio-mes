package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class LiuduLayoutBuildService extends AbstractLayoutModeBuildService {
    private static final int EXPAND_TOP_MM = 20;
    private static final int EXPAND_LEFT_MM = 21;
    private static final int EXPAND_RIGHT_MM = 13;
    private static final int GAP_TO_RIGHT_MM = 10;

    private final OssTagUploadService ossTagUploadService;
    private final RestTemplate restTemplate = new RestTemplate();

    public LiuduLayoutBuildService(OssTagUploadService ossTagUploadService) {
        this.ossTagUploadService = ossTagUploadService;
    }

    /** XY切割-六渡大板模式构建器。 */
    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_LIUDU_LARGE_BOARD;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        String svgContent = fetchSvgContent(context.getTypesettingInfo().getElement().getNestedSvg());
        LeftBottomCoordinate leftBottom = parseFormeLeftBottomCoordinate(svgContent);
        LeftBottomCoordinate leftTop = parseFormeLeftTopCoordinate(svgContent);

        int originalWidth = context.getNestedWidth().intValue();
        int originalHeight = context.getNestedHeight().intValue();
        int finalWidth = originalWidth + EXPAND_LEFT_MM + EXPAND_RIGHT_MM;
        int finalHeight = originalHeight + EXPAND_TOP_MM;

        FormeLayoutBuildResult result = new FormeLayoutBuildResult();
        FormeGenerationRequest.Margin margin = new FormeGenerationRequest.Margin();
        margin.setLeft(EXPAND_LEFT_MM);
        margin.setTop(EXPAND_TOP_MM);
        margin.setRight(EXPAND_RIGHT_MM);
        margin.setBottom(0);
        result.setMargin(margin);

        String elementA = ossTagUploadService.uploadLagPng(context.getBusinessId(), createBlackPng(3, finalHeight));
        String elementB = ossTagUploadService.uploadLagPng(context.getBusinessId(), createBlackPng(10, 10));
        String elementC = ossTagUploadService.uploadLagPng(context.getBusinessId(), createBlackPng(5, 10));

        List<FormeGenerationRequest.Mark> marks = new ArrayList<>();
        // Step6: A at x=8,y=0
        marks.add(createMark(elementA, 3, finalHeight, 8, 0));
        // Step9: duplicate A to right side
        marks.add(createMark(elementA, 3, finalHeight, 13 + originalWidth + GAP_TO_RIGHT_MM, 0));

        // Step5: C based on left-bottom coordinate, put left 21 and up 500, overflow no place
        double originRightX = finalWidth;
        double lbXFromRight = originRightX - (leftBottom.getX().doubleValue() + EXPAND_LEFT_MM);
        double lbYFromTop = leftBottom.getY().doubleValue() + EXPAND_TOP_MM;
        double cRightBasedX = lbXFromRight + EXPAND_LEFT_MM;
        double cY = lbYFromTop - 500;
        int cWidth = 5;
        if (cRightBasedX + cWidth <= finalWidth && cY >= 0) {
            int cLeftTopX = (int) Math.round(finalWidth - cRightBasedX - cWidth);
            marks.add(createMark(elementC, 5, 10, cLeftTopX, (int) Math.round(cY)));
        }

        // Step7: B based on left-top coordinate (left10, down60 / down80)
        double ltXFromRight = originRightX - (leftTop.getX().doubleValue() + EXPAND_LEFT_MM);
        double ltYFromTop = leftTop.getY().doubleValue() + EXPAND_TOP_MM;
        List<FormeGenerationRequest.Mark> bMarks = new ArrayList<>();
        addBMark(bMarks, elementB, finalWidth, ltXFromRight + 10, ltYFromTop + 60);
        addBMark(bMarks, elementB, finalWidth, ltXFromRight + 10, ltYFromTop + 80);
        marks.addAll(bMarks);

        // Step8: duplicate B to right of original svg (+width +10)
        for (FormeGenerationRequest.Mark mark : bMarks) {
            FormeGenerationRequest.Position p = mark.getPosition();
            marks.add(createMark(elementB, 10, 10, p.getX() + originalWidth + GAP_TO_RIGHT_MM, p.getY()));
        }

        result.setMarks(marks);
        result.setAnchorPoints(Collections.emptyList());
        result.setOutputs(buildDefaultOutputs(supportMode(), context));
        result.setUploadPath("forme/" + context.getBusinessId() + "/");
        return result;
    }

    private void addBMark(List<FormeGenerationRequest.Mark> marks, String img, int finalWidth, double rightBasedX, double y) {
        int width = 10;
        int leftTopX = (int) Math.round(finalWidth - rightBasedX - width);
        marks.add(createMark(img, width, 10, leftTopX, (int) Math.round(y)));
    }

    private FormeGenerationRequest.Mark createMark(String img, int width, int height, int x, int y) {
        FormeGenerationRequest.Mark mark = new FormeGenerationRequest.Mark();
        mark.setImg(img);
        mark.setSize(createSize(BigDecimal.valueOf(width), BigDecimal.valueOf(height)));
        mark.setPosition(createPosition(Math.max(0, x), Math.max(0, y)));
        return mark;
    }

    private byte[] createBlackPng(int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, width, height);
            g.dispose();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("生成黑色 PNG 失败", e);
        }
    }

    private String fetchSvgContent(String nestedSvg) {
        if (StringUtils.isBlank(nestedSvg)) {
            throw new IllegalArgumentException("nestedSvg 不能为空");
        }
        try {
            byte[] bytes = restTemplate.getForObject(URI.create(nestedSvg), byte[].class);
            if (bytes == null || bytes.length == 0) {
                throw new IllegalArgumentException("无法下载 nestedSvg 内容");
            }
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("读取 nestedSvg 失败: " + nestedSvg, e);
        }
    }
}

package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class CaifuOpenBackA30HFilmLayoutBuildService extends CaifuLayoutBuildService {
    private static final int EXPAND_TOP_MM = 3;
    private static final int EXPAND_RIGHT_MM = 11;

    private static final int ELEMENT_A_WIDTH_MM = 3;
    private static final int ELEMENT_B_WIDTH_MM = 8;
    private static final int ELEMENT_B_HEIGHT_MM = 3;
    private static final int ELEMENT_A_OFFSET_Y_MM = 295;
    private static final int ELEMENT_B_X_OFFSET_MM = 8;
    private static final int ELEMENT_D_WIDTH_TENTH_MM = 3;
    private static final int ELEMENT_D_HEIGHT_MM = 5;
    private static final int ELEMENT_D_OFFSET_Y_MM = 8;
    private static final int ELEMENT_D_OFFSET_RIGHT_ONE_TENTH_MM = 203;
    private static final int ELEMENT_D_OFFSET_RIGHT_TWO_TENTH_MM = 303;

    public CaifuOpenBackA30HFilmLayoutBuildService(OssTagUploadService ossTagUploadService) {
        super(ossTagUploadService);
    }

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30H_FILM;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        int originalWidth = context.getNestedWidth().intValue();
        int originalHeight = context.getNestedHeight().intValue();
        int expandedHeight = originalHeight + EXPAND_TOP_MM;

        FormeLayoutBuildResult result = new FormeLayoutBuildResult();
        FormeGenerationRequest.Margin margin = new FormeGenerationRequest.Margin();
        margin.setLeft(0);
        margin.setTop(EXPAND_TOP_MM);
        margin.setRight(EXPAND_RIGHT_MM);
        margin.setBottom(0);
        result.setMargin(margin);

        String tagUploadSubDir = buildTagUploadSubDir(context);
        String elementA = ossTagUploadService.uploadTagPng(
                context.getBusinessId(),
                createBlackPng(ELEMENT_A_WIDTH_MM, expandedHeight),
                tagUploadSubDir
        );
        String elementB = ossTagUploadService.uploadTagPng(
                context.getBusinessId(),
                createBlackPng(ELEMENT_B_WIDTH_MM, ELEMENT_B_HEIGHT_MM),
                tagUploadSubDir
        );
        String elementD = ossTagUploadService.uploadTagPng(
                context.getBusinessId(),
                createBlackPng(ELEMENT_D_WIDTH_TENTH_MM / 10.0, ELEMENT_D_HEIGHT_MM),
                tagUploadSubDir
        );
        String elementE = ossTagUploadService.uploadTagPng(
                context.getBusinessId(),
                createWhitePng(0.8, ELEMENT_D_HEIGHT_MM),
                tagUploadSubDir
        );

        LinkedHashSet<Double> ys = new LinkedHashSet<>();
        ys.add(0D);
        TypesettingElement.GridLines gridLines = context.getTypesettingInfo() != null
                && context.getTypesettingInfo().getElement() != null
                ? context.getTypesettingInfo().getElement().getGridLines()
                : null;
        if (gridLines != null && gridLines.getYs() != null) {
            ys.addAll(gridLines.getYs());
        }

        List<FormeGenerationRequest.Mark> marks = new ArrayList<>();
        for (Double y : ys) {
            if (y == null) {
                continue;
            }
            int elementAY = (int) Math.round(y + ELEMENT_A_OFFSET_Y_MM);
            if (elementAY > expandedHeight) {
                continue;
            }
            marks.add(createMark(elementA, ELEMENT_A_WIDTH_MM, expandedHeight, originalWidth, elementAY));

            int elementDY = (int) Math.round(y + ELEMENT_D_OFFSET_Y_MM);
            if (elementDY <= expandedHeight) {
                marks.add(createMark(elementD,
                        ELEMENT_D_WIDTH_TENTH_MM / 10.0,
                        ELEMENT_D_HEIGHT_MM,
                        originalWidth + EXPAND_RIGHT_MM - (ELEMENT_D_OFFSET_RIGHT_ONE_TENTH_MM / 10.0),
                        elementDY));
                marks.add(createMark(elementD,
                        ELEMENT_D_WIDTH_TENTH_MM / 10.0,
                        ELEMENT_D_HEIGHT_MM,
                        originalWidth + EXPAND_RIGHT_MM - (ELEMENT_D_OFFSET_RIGHT_TWO_TENTH_MM / 10.0),
                        elementDY));
            }
        }
        marks.add(createMark(elementB, ELEMENT_B_WIDTH_MM, ELEMENT_B_HEIGHT_MM, originalWidth + ELEMENT_B_X_OFFSET_MM, 0));

        if (context.getTypesettingInfo() != null) {
            LinkedHashMap<String, String> markFiles = new LinkedHashMap<>();
            markFiles.put("elementA", elementA);
            markFiles.put("elementB", elementB);
            markFiles.put("elementD", elementD);
            markFiles.put("elementE", elementE);
            context.getTypesettingInfo().setMarks(markFiles);
        }

        result.setMarks(marks);
        result.setAnchorPoints(Collections.emptyList());
        result.setOutputs(buildDefaultOutputs(supportMode(), context));
        result.setUploadPath("forme/" + context.getBusinessId() + "/");
        return result;
    }

    protected FormeGenerationRequest.Mark createMark(String img, double width, double height, double x, double y) {
        FormeGenerationRequest.Mark mark = new FormeGenerationRequest.Mark();
        mark.setImg(img);
        mark.setSize(createSize(java.math.BigDecimal.valueOf(width), java.math.BigDecimal.valueOf(height)));
        mark.setPosition(createPosition((int) Math.max(0, Math.round(x)), (int) Math.max(0, Math.round(y))));
        return mark;
    }

    private byte[] createWhitePng(double width, double height) {
        try {
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage((int) Math.ceil(width), (int) Math.ceil(height), java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = image.createGraphics();
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.dispose();
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("生成白色 PNG 失败", e);
        }
    }

    private byte[] createBlackPng(double width, double height) {
        try {
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage((int) Math.ceil(width), (int) Math.ceil(height), java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = image.createGraphics();
            g.setColor(java.awt.Color.BLACK);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.dispose();
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("生成黑色 PNG 失败", e);
        }
    }
}

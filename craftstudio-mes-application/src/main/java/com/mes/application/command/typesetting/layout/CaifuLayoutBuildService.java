package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class CaifuLayoutBuildService extends AbstractLayoutModeBuildService {
    private static final int EXPAND_LEFT_MM = 10;
    private static final int EXPAND_RIGHT_MM = 13;

    private static final int MARK_B_WIDTH_MM = 8;
    private static final int MARK_B_HEIGHT_MM = 3;
    private static final int MARK_B_OFFSET_Y_MM = 255;

    private static final int MARK_C_WIDTH_MM = 3;
    private static final int MARK_C_OFFSET_X_MM = 20;

    private final OssTagUploadService ossTagUploadService;

    public CaifuLayoutBuildService(OssTagUploadService ossTagUploadService) {
        this.ossTagUploadService = ossTagUploadService;
    }

    /** XY切割-裁赋模式构建器。 */
    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_A20PR0;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        int originalWidth = context.getNestedWidth().intValue();
        int originalHeight = context.getNestedHeight().intValue();
        int expandedHeight = originalHeight;

        FormeLayoutBuildResult result = new FormeLayoutBuildResult();
        FormeGenerationRequest.Margin margin = new FormeGenerationRequest.Margin();
        margin.setLeft(EXPAND_LEFT_MM);
        margin.setTop(0);
        margin.setRight(EXPAND_RIGHT_MM);
        margin.setBottom(0);
        result.setMargin(margin);
        String tagUploadSubDir = buildTagUploadSubDir(context);

        String elementB = ossTagUploadService.uploadTagPng(
                context.getBusinessId(),
                createBlackPng(MARK_B_WIDTH_MM, MARK_B_HEIGHT_MM),
                tagUploadSubDir
        );
        String elementC = ossTagUploadService.uploadTagPng(
                context.getBusinessId(),
                createBlackPng(MARK_C_WIDTH_MM, expandedHeight),
                tagUploadSubDir
        );

        List<FormeGenerationRequest.Mark> marks = new ArrayList<>();

        LinkedHashSet<Double> ys = new LinkedHashSet<>();
        ys.add(0D);
        TypesettingElement.GridLines gridLines = context.getTypesettingInfo() != null
                && context.getTypesettingInfo().getElement() != null
                ? context.getTypesettingInfo().getElement().getGridLines()
                : null;
        if (gridLines != null && gridLines.getYs() != null) {
            ys.addAll(gridLines.getYs());
        }

        int rightBMarkX = EXPAND_LEFT_MM + originalWidth;
        for (Double y : ys) {
            if (y == null) {
                continue;
            }
            if (y + MARK_B_OFFSET_Y_MM > expandedHeight) {
                continue;
            }
            int bMarkY = (int) Math.round(y + MARK_B_OFFSET_Y_MM);
            marks.add(createMark(elementB, MARK_B_WIDTH_MM, MARK_B_HEIGHT_MM, 0, bMarkY));
            marks.add(createMark(elementB, MARK_B_WIDTH_MM, MARK_B_HEIGHT_MM, rightBMarkX, bMarkY));
        }

        marks.add(createMark(
                elementC,
                MARK_C_WIDTH_MM,
                expandedHeight,
                MARK_C_OFFSET_X_MM + originalWidth,
                0
        ));

        if (context.getTypesettingInfo() != null) {
            LinkedHashMap<String, String> markFiles = new LinkedHashMap<>();
            markFiles.put("elementB", elementB);
            markFiles.put("elementC", elementC);
            context.getTypesettingInfo().setMarks(markFiles);
        }

        result.setMarks(marks);
        result.setAnchorPoints(Collections.emptyList());
        result.setOutputs(buildDefaultOutputs(supportMode(), context));
        result.setUploadPath("forme/" + context.getBusinessId() + "/");
        return result;
    }

    private String buildTagUploadSubDir(FormeBuildContext context) {
        String manufacturerMetaId = context.getTypesettingInfo() == null ? null : context.getTypesettingInfo().getManufacturerMetaId();
        String typesettingId = context.getTypesettingInfo() == null ? null : context.getTypesettingInfo().getTypesettingId();
        if (isBlank(manufacturerMetaId) || isBlank(typesettingId)) {
            return "mark";
        }
        return "mark/" + manufacturerMetaId + "/" + typesettingId;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
}

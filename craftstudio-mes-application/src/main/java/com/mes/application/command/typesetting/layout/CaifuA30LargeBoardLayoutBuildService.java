package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class CaifuA30LargeBoardLayoutBuildService extends CaifuLayoutBuildService {
    private static final int EXPAND_TOP_MM = 20;
    private static final int EXPAND_LEFT_MM = 8;
    private static final int EXPAND_RIGHT_MM = 5;

    private static final int MARK_A_WIDTH_MM = 3;
    private static final int MARK_C_OFFSET_X_MM = 2;
    private static final int MARK_B_WIDTH_MM = 8;
    private static final int MARK_B_HEIGHT_MM = 3;
    private static final int MARK_B_OFFSET_LEFT_MM = 8;
    private static final int MARK_B_OFFSET_UP_MM = 438;

    private final OssTagUploadService ossTagUploadService;

    public CaifuA30LargeBoardLayoutBuildService(OssTagUploadService ossTagUploadService) {
        super(ossTagUploadService);
        this.ossTagUploadService = ossTagUploadService;
    }

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_A30_LARGE_BOARD;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        int originalWidth = context.getNestedWidth().intValue();
        int originalHeight = context.getNestedHeight().intValue();
        int expandedHeight = originalHeight + EXPAND_TOP_MM;

        FormeLayoutBuildResult result = new FormeLayoutBuildResult();
        FormeGenerationRequest.Margin margin = new FormeGenerationRequest.Margin();
        margin.setLeft(EXPAND_LEFT_MM);
        margin.setTop(EXPAND_TOP_MM);
        margin.setRight(EXPAND_RIGHT_MM);
        margin.setBottom(0);
        result.setMargin(margin);

        String tagUploadSubDir = buildTagUploadSubDir(context);
        String elementA = ossTagUploadService.uploadTagPng(
                context.getBusinessId(),
                createBlackPng(MARK_A_WIDTH_MM, expandedHeight),
                tagUploadSubDir
        );
        String elementB = ossTagUploadService.uploadTagPng(
                context.getBusinessId(),
                createBlackPng(MARK_B_WIDTH_MM, MARK_B_HEIGHT_MM),
                tagUploadSubDir
        );

        List<FormeGenerationRequest.Mark> marks = new ArrayList<>();
        marks.add(createMark(
                elementA,
                MARK_A_WIDTH_MM,
                expandedHeight,
                MARK_C_OFFSET_X_MM + originalWidth,
                0
        ));

        List<CellOrigin> cellOrigins = extractCellOrigins(context.getTypesettingInfo());
        List<TypesettingSourceCell> typesettingCells = context.getTypesettingInfo() == null
                ? null : context.getTypesettingInfo().getTypesettingCells();
        if (typesettingCells != null && !typesettingCells.isEmpty()) {
            int count = Math.min(typesettingCells.size(), cellOrigins.size());
            for (int i = 0; i < count; i++) {
                CellOrigin origin = cellOrigins.get(i);
                int bX = origin.getX() - MARK_B_OFFSET_LEFT_MM;
                int bY = origin.getY() + MARK_B_OFFSET_UP_MM;
                if (bY + MARK_B_HEIGHT_MM > expandedHeight) {
                    continue;
                }
                marks.add(createMark(elementB, MARK_B_WIDTH_MM, MARK_B_HEIGHT_MM, bX, bY));
            }
        }

        if (context.getTypesettingInfo() != null) {
            LinkedHashMap<String, String> markFiles = new LinkedHashMap<>();
            markFiles.put("elementA", elementA);
            markFiles.put("elementC", elementA);
            markFiles.put("elementB", elementB);
            context.getTypesettingInfo().setMarks(markFiles);
        }

        result.setMarks(marks);
        result.setAnchorPoints(Collections.emptyList());
        result.setOutputs(buildDefaultOutputs(supportMode(), context));
        result.setUploadPath("forme/" + context.getBusinessId() + "/");
        return result;
    }

    private List<CellOrigin> extractCellOrigins(TypesettingInfo typesettingInfo) {
        if (typesettingInfo == null || typesettingInfo.getElement() == null || typesettingInfo.getElement().getGridLines() == null) {
            return Collections.singletonList(new CellOrigin(0, 0));
        }
        TypesettingElement.GridLines gridLines = typesettingInfo.getElement().getGridLines();
        List<Double> xs = gridLines.getXs();
        List<Double> ys = gridLines.getYs();
        if (xs == null || xs.isEmpty() || ys == null || ys.isEmpty()) {
            return Collections.singletonList(new CellOrigin(0, 0));
        }
        List<Double> sortedXs = new ArrayList<>(xs);
        List<Double> sortedYs = new ArrayList<>(ys);
        sortedXs.sort(Comparator.naturalOrder());
        sortedYs.sort(Comparator.naturalOrder());
        List<CellOrigin> origins = new ArrayList<>();
        for (Double y : sortedYs) {
            if (y == null) {
                continue;
            }
            for (Double x : sortedXs) {
                if (x == null) {
                    continue;
                }
                origins.add(new CellOrigin((int) Math.round(x), (int) Math.round(y)));
            }
        }
        return origins.isEmpty() ? Collections.singletonList(new CellOrigin(0, 0)) : origins;
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

    private static class CellOrigin {
        private final int x;
        private final int y;

        private CellOrigin(int x, int y) {
            this.x = x;
            this.y = y;
        }

        private int getX() {
            return x;
        }

        private int getY() {
            return y;
        }
    }
}

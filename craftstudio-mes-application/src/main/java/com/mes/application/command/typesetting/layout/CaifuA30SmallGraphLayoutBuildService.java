package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class CaifuA30SmallGraphLayoutBuildService extends CaifuLayoutBuildService {
    /**
     * XY切割（切割辅助线-裁赋A30小图）规则备注：
     * 1) 在原svg上扩边：上20mm、右11mm，扩边后左上角作为原点；
     * 2) 元素A：读取当前排版 gridLines.ys，并额外补一条 y=20；
     * 3) 元素B：8x3mm 黑色PNG；
     * 4) 元素C：3mm宽、与扩边后图形同高的黑色PNG；
     * 5) 遍历元素A的 y：在 x=原svg.width，y+244 处放置元素B，若 y+274 > 原svg.height 则不放；
     * 6) 在 x=(8+svg.width), y=0 放置元素C；
     * 7) 遍历 typesettingCells 中 sourceType=typesetting，读取其对应 typesettingInfo 作为元素D，
     *    取元素D的 gridLines.ys，并补一条 y=0；
     * 8) 遍历元素D的 y：在 x=元素D.svg.width，y+294 处放置元素B，若 y+294 > 原svg.height 则不放。
     */
    private static final int EXPAND_TOP_MM = 20;
    private static final int EXPAND_RIGHT_MM = 11;

    private static final int MARK_B_WIDTH_MM = 8;
    private static final int MARK_B_HEIGHT_MM = 3;

    private static final int MARK_B_OFFSET_Y_FROM_A_MM = 244;
    private static final int MARK_B_MAX_OFFSET_Y_FROM_A_MM = 274;

    private static final int MARK_B_OFFSET_Y_FROM_D_MM = 294;

    private static final int MARK_C_WIDTH_MM = 3;
    private static final int MARK_C_OFFSET_X_MM = 8;

    private final OssTagUploadService ossTagUploadService;
    private final TypesettingService typesettingService;

    public CaifuA30SmallGraphLayoutBuildService(OssTagUploadService ossTagUploadService,
                                                 TypesettingService typesettingService) {
        super(ossTagUploadService);
        this.ossTagUploadService = ossTagUploadService;
        this.typesettingService = typesettingService;
    }

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_A30_SMALL_GRAPH;
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

        // 元素A（当前排版）：gridLines.ys + 顶部辅助线 y=20
        LinkedHashSet<Double> elementAYs = extractElementAYs(context.getTypesettingInfo());
        for (Double y : elementAYs) {
            if (y == null || y + MARK_B_MAX_OFFSET_Y_FROM_A_MM > originalHeight) {
                continue;
            }
            int markY = (int) Math.round(y + MARK_B_OFFSET_Y_FROM_A_MM);
            // x=原svg.width，y+244 放置元素B；超过 y+274 边界时不放
            marks.add(createMark(elementB, MARK_B_WIDTH_MM, MARK_B_HEIGHT_MM, originalWidth, markY));
        }

        // 元素C：x=(8+svg.width)，y=0，高度=扩边后高度
        marks.add(createMark(
                elementC,
                MARK_C_WIDTH_MM,
                expandedHeight,
                MARK_C_OFFSET_X_MM + originalWidth,
                0
        ));

        if (context.getTypesettingInfo() != null && context.getTypesettingInfo().getTypesettingCells() != null) {
            for (TypesettingSourceCell cell : context.getTypesettingInfo().getTypesettingCells()) {
                TypesettingInfo nested = resolveTypesettingCellInfo(cell);
                if (nested == null || nested.getElement() == null || nested.getElement().getWidth() == null) {
                    continue;
                }
                int nestedWidth = nested.getElement().getWidth().intValue();
                // 元素D（嵌套排版）：gridLines.ys + y=0
                LinkedHashSet<Double> nestedYs = extractElementDYs(nested.getElement().getGridLines());
                for (Double y : nestedYs) {
                    if (y == null || y + MARK_B_OFFSET_Y_FROM_D_MM > originalHeight) {
                        continue;
                    }
                    int markY = (int) Math.round(y + MARK_B_OFFSET_Y_FROM_D_MM);
                    // x=元素D.svg.width，y+294 放置元素B
                    marks.add(createMark(elementB, MARK_B_WIDTH_MM, MARK_B_HEIGHT_MM, nestedWidth, markY));
                }
            }
        }

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

    private LinkedHashSet<Double> extractElementAYs(TypesettingInfo typesettingInfo) {
        LinkedHashSet<Double> ys = new LinkedHashSet<>();
        ys.add((double) EXPAND_TOP_MM);
        if (typesettingInfo == null || typesettingInfo.getElement() == null || typesettingInfo.getElement().getGridLines() == null) {
            return ys;
        }
        List<Double> gridYs = typesettingInfo.getElement().getGridLines().getYs();
        if (gridYs != null) {
            ys.addAll(gridYs);
        }
        return ys;
    }

    private LinkedHashSet<Double> extractElementDYs(TypesettingElement.GridLines gridLines) {
        LinkedHashSet<Double> ys = new LinkedHashSet<>();
        ys.add(0D);
        if (gridLines != null && gridLines.getYs() != null) {
            ys.addAll(gridLines.getYs());
        }
        return ys;
    }

    private TypesettingInfo resolveTypesettingCellInfo(TypesettingSourceCell cell) {
        if (cell == null || StringUtils.isBlank(cell.getSourceType()) || StringUtils.isBlank(cell.getSourceId())) {
            return null;
        }
        boolean isTypesettingSource = TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType())
                || "typesetting".equalsIgnoreCase(cell.getSourceType());
        if (!isTypesettingSource) {
            return null;
        }
        List<TypesettingInfo> infos = typesettingService.findTypesettingListByTypesettingId(cell.getSourceId());
        if (infos != null && !infos.isEmpty() && infos.get(0) != null) {
            return infos.get(0);
        }
        return typesettingService.findById(cell.getSourceId());
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

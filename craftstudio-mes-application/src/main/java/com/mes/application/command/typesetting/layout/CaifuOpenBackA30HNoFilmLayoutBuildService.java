package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.layout.CaifuMarkerBands.Band;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class CaifuOpenBackA30HNoFilmLayoutBuildService extends CaifuLayoutBuildService {
    protected static final int EXPAND_TOP_MM = 3;
    protected static final int EXPAND_LEFT_MM = 11;

    private static final int ELEMENT_A_WIDTH_MM = 3;
    protected static final int ELEMENT_B_WIDTH_MM = 8;
    protected static final int ELEMENT_B_HEIGHT_MM = 3;
    private static final int ELEMENT_B_OFFSET_Y_MM = 295;
    private static final int ELEMENT_B_X_MM = 3;
    private static final int ELEMENT_D_WIDTH_TENTH_MM = 3;
    private static final int ELEMENT_D_HEIGHT_MM = 5;
    private static final int ELEMENT_D_OFFSET_Y_MM = 8;
    private static final int ELEMENT_D_OFFSET_RIGHT_ZERO_TENTH_MM = 5;
    private static final int ELEMENT_D_OFFSET_RIGHT_ONE_TENTH_MM = 203;
    private static final int ELEMENT_D_OFFSET_RIGHT_TWO_TENTH_MM = 303;

    private final TypesettingService typesettingService;

    public CaifuOpenBackA30HNoFilmLayoutBuildService(OssTagUploadService ossTagUploadService,
                                                     TypesettingService typesettingService) {
        super(ossTagUploadService);
        this.typesettingService = typesettingService;
    }

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30H_NO_FILM;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        int originalWidth = context.getNestedWidth().intValue();
        int originalHeight = context.getNestedHeight().intValue();
        int expandedHeight = originalHeight + EXPAND_TOP_MM;
        int expandedWidth = originalWidth + expandLeftMm() + expandRightMm();
        // 右侧新增区域不改变 D / 非出血黑线相对原版的位置。
        int lineRightEdge = expandedWidth - expandRightMm();
        int contentOffsetY = openBackTagHeightMm();

        FormeLayoutBuildResult result = new FormeLayoutBuildResult();
        FormeGenerationRequest.Margin margin = new FormeGenerationRequest.Margin();
        margin.setLeft(expandLeftMm());
        margin.setTop(EXPAND_TOP_MM + contentOffsetY);
        margin.setRight(expandRightMm());
        margin.setBottom(contentOffsetY);
        result.setMargin(margin);

        List<Band> orderedBands = new ArrayList<>(CaifuMarkerBands.read(context.getTypesettingInfo()));
        orderedBands.add(new Band("ysBottom", expandedHeight, ELEMENT_D_HEIGHT_MM, null));
        boolean hasBloodBand = orderedBands.stream().anyMatch(this::isBloodBand);

        String tagUploadSubDir = buildTagUploadSubDir(context);
        String elementA = ossTagUploadService.uploadTagPng(context.getBusinessId(), createBlackPng(ELEMENT_A_WIDTH_MM, expandedHeight), tagUploadSubDir);
        String elementB = ossTagUploadService.uploadTagPng(context.getBusinessId(), createBlackPng(ELEMENT_B_WIDTH_MM, ELEMENT_B_HEIGHT_MM), tagUploadSubDir);
        String elementD = ossTagUploadService.uploadTagPng(context.getBusinessId(), createBlackPng(ELEMENT_D_WIDTH_TENTH_MM / 10.0, ELEMENT_D_HEIGHT_MM), tagUploadSubDir);
        String elementF = contentOffsetY > 0 ? buildOpenBackTagStrip(context, originalWidth, false) : null;
        String elementFRotated = contentOffsetY > 0 ? buildOpenBackTagStrip(context, originalWidth, true) : null;

        Map<Integer, String> elementEByHeight = new HashMap<>();
        List<FormeGenerationRequest.Mark> marks = new ArrayList<>();
        if (contentOffsetY > 0) {
            marks.add(createMark(elementF, originalWidth, contentOffsetY, expandLeftMm(), 0));
            marks.add(createMark(elementFRotated, originalWidth, contentOffsetY, expandLeftMm(), contentOffsetY + expandedHeight));
        }
        marks.add(createMark(elementA, ELEMENT_A_WIDTH_MM, expandedHeight, 0, contentOffsetY));

        addElementBMarks(context, marks, elementB, expandedHeight, contentOffsetY, orderedBands);

        for (Band band : orderedBands) {
            if (band == null || "ysBottom".equals(band.id())) {
                continue;
            }
            double lineY = band.centerY() + ELEMENT_D_OFFSET_Y_MM;
            if (lineY > expandedHeight) {
                continue;
            }
            if (!hasBloodBand) {
                continue;
            }
            boolean isBlood = isBloodBand(band);
            String lineImg = elementD;
            double lineHeight = ELEMENT_D_HEIGHT_MM;
            if (!isBlood) {
                int svgHeight = (int) Math.max(1, Math.round(band.height()) - 30);
                lineImg = elementEByHeight.computeIfAbsent(svgHeight,
                        h -> ossTagUploadService.uploadTagPng(context.getBusinessId(), createBlackPng(ELEMENT_D_WIDTH_TENTH_MM / 10.0, h), tagUploadSubDir));
                lineHeight = svgHeight;
            }
            if (!isBlood) {
                marks.add(createMark(lineImg, ELEMENT_D_WIDTH_TENTH_MM / 10.0, lineHeight,
                        lineRightEdge - (ELEMENT_D_OFFSET_RIGHT_ZERO_TENTH_MM / 10.0), lineY + contentOffsetY));
            }
            marks.add(createMark(lineImg, ELEMENT_D_WIDTH_TENTH_MM / 10.0, lineHeight,
                    lineRightEdge - (ELEMENT_D_OFFSET_RIGHT_ONE_TENTH_MM / 10.0), lineY + contentOffsetY));
            marks.add(createMark(lineImg, ELEMENT_D_WIDTH_TENTH_MM / 10.0, lineHeight,
                    lineRightEdge - (ELEMENT_D_OFFSET_RIGHT_TWO_TENTH_MM / 10.0), lineY + contentOffsetY));
            if (isBlood) {
                // 右下角标记固定距当前 cell 底边 8 mm，不能使用整张版面的底边，
                // 否则多个出血 cell 的下方标记都会错误地堆叠到版面底部。
                double bottomLineY = calculateBottomLineY(band.centerY(), band.height(), lineHeight);
                if (bottomLineY >= 0 && Math.abs(bottomLineY - lineY) > 0.0001) {
                    marks.add(createMark(lineImg, ELEMENT_D_WIDTH_TENTH_MM / 10.0, lineHeight,
                            lineRightEdge - (ELEMENT_D_OFFSET_RIGHT_ONE_TENTH_MM / 10.0), bottomLineY + contentOffsetY));
                    marks.add(createMark(lineImg, ELEMENT_D_WIDTH_TENTH_MM / 10.0, lineHeight,
                            lineRightEdge - (ELEMENT_D_OFFSET_RIGHT_TWO_TENTH_MM / 10.0), bottomLineY + contentOffsetY));
                }
            }
        }

        if (context.getTypesettingInfo() != null) {
            LinkedHashMap<String, String> markFiles = new LinkedHashMap<>();
            markFiles.put("elementA", elementA);
            markFiles.put("elementB", elementB);
            markFiles.put("elementD", elementD);
            if (contentOffsetY > 0) {
                markFiles.put("elementF", elementF);
                markFiles.put("elementFRotated", elementFRotated);
            }
            if (!elementEByHeight.isEmpty()) {
                markFiles.put("elementE", elementEByHeight.values().iterator().next());
            }
            context.getTypesettingInfo().setMarks(markFiles);
        }

        result.setMarks(marks);
        result.setAnchorPoints(Collections.emptyList());
        result.setOutputs(buildDefaultOutputs(supportMode(), context));
        result.setUploadPath("forme/" + context.getBusinessId() + "/");
        return result;
    }

    protected int expandLeftMm() {
        return EXPAND_LEFT_MM;
    }

    protected int expandRightMm() {
        return 0;
    }

    protected int openBackTagHeightMm() {
        return OPEN_BACK_TAG_HEIGHT_MM;
    }

    protected void addElementBMarks(FormeBuildContext context,
                                    List<FormeGenerationRequest.Mark> marks,
                                    String elementB,
                                    int expandedHeight,
                                    int contentOffsetY,
                                    List<Band> orderedBands) {
        LinkedHashSet<Integer> placedElementBYs = new LinkedHashSet<>();
        for (Band band : orderedBands) {
            if (band == null) {
                continue;
            }
            int elementBY = (int) Math.round(band.centerY() - ELEMENT_B_OFFSET_Y_MM);
            if (elementBY < 0 || elementBY > expandedHeight || !placedElementBYs.add(elementBY)) {
                continue;
            }
            marks.add(createMark(elementB, ELEMENT_B_WIDTH_MM, ELEMENT_B_HEIGHT_MM, ELEMENT_B_X_MM, elementBY + contentOffsetY));
        }
    }

    static double calculateBottomLineY(double cellTopY, double cellHeight, double lineHeight) {
        return cellTopY + cellHeight - ELEMENT_D_OFFSET_Y_MM - lineHeight;
    }

    private boolean isBloodBand(Band band) {
        if (band == null || StringUtils.isBlank(band.relatedTypesettingId())) {
            return false;
        }
        TypesettingInfo info = typesettingService.findById(band.relatedTypesettingId());
        if (info != null) {
            return Boolean.TRUE.equals(info.getHaveBlood());
        }
        List<TypesettingInfo> infos = typesettingService.findTypesettingListByTypesettingId(band.relatedTypesettingId());
        return infos != null && !infos.isEmpty() && infos.get(0) != null && Boolean.TRUE.equals(infos.get(0).getHaveBlood());
    }

    protected FormeGenerationRequest.Mark createMark(String img, double width, double height, double x, double y) {
        FormeGenerationRequest.Mark mark = new FormeGenerationRequest.Mark();
        mark.setImg(img);
        mark.setSize(createSize(java.math.BigDecimal.valueOf(width), java.math.BigDecimal.valueOf(height)));
        mark.setPosition(createPosition((int) Math.max(0, Math.round(x)), (int) Math.max(0, Math.round(y))));
        return mark;
    }

    private com.mes.application.command.graphics.RemotePng createBlackPng(double width, double height) {
        return com.mes.application.command.graphics.RemoteGraphics.png("BLACK", com.mes.application.command.graphics.RemoteGraphics.params("width", width, "height", height));
    }

}

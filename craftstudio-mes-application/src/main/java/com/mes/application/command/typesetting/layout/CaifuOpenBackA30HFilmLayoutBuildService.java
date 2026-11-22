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
import java.util.List;
import java.util.Map;

/**
 * XY切割-裁赋开背A30H覆膜模式构建器。
 *
 * <p>核心规则（业务约定）
 * <ul>
 *   <li>原图上扩3mm、右扩11mm，以扩边后左上角为原点；</li>
 *   <li>元素A：3mm宽、与扩边后同高；元素B：8x3mm；元素D：0.3x5mm；</li>
 *   <li>ys 由 typesetting.marks 中 caifuMarker_* 对应 nestedSvg 节点中值推导，并补 ys=0；</li>
 *   <li>在 y+295 放 B；在 y+8 放 D/E（由是否出血印版决定）；</li>
 * </ul>
 *
 * <p><b>重点：如何拿到“ys 下方后继 &lt;g&gt; 的 id”</b>
 * <ol>
 *   <li>先从 marks 的 caifuMarker_* 提取文件名（去后缀）得到 markerId；</li>
 *   <li>在 nestedSvg 中定位 id=markerId 的 marker 节点（通常是顶层 &lt;g&gt;）；</li>
 *   <li>按 SVG 根节点顶层 g 顺序，找到该 marker 所在位置；</li>
 *   <li>取其后一个满足印版组条件的顶层 g（data-forme=true 或 data-source-name 以 .svg 结尾）；</li>
 *   <li>这个后继 g 的 id 即 relatedTypesettingId，用于 findById 后判断 haveBlood。</li>
 * </ol>
 */
@Service
public class CaifuOpenBackA30HFilmLayoutBuildService extends CaifuLayoutBuildService {
    private static final int EXPAND_TOP_MM = 3;
    private static final int EXPAND_RIGHT_MM = 11;
    private static final int ELEMENT_A_WIDTH_MM = 3;
    private static final int ELEMENT_B_WIDTH_MM = 8;
    private static final int ELEMENT_B_HEIGHT_MM = 3;
    private static final int ELEMENT_B_OFFSET_Y_MM = 295;
    private static final int ELEMENT_A_X_OFFSET_MM = 8;
    private static final int ELEMENT_D_WIDTH_TENTH_MM = 3;
    private static final int ELEMENT_D_HEIGHT_MM = 5;
    private static final int ELEMENT_D_OFFSET_Y_MM = 8;
    private static final int ELEMENT_D_OFFSET_RIGHT_ZERO_TENTH_MM = 5;
    private static final int ELEMENT_D_OFFSET_RIGHT_ONE_TENTH_MM = 203;
    private static final int ELEMENT_D_OFFSET_RIGHT_TWO_TENTH_MM = 303;

    private final TypesettingService typesettingService;
    public CaifuOpenBackA30HFilmLayoutBuildService(OssTagUploadService ossTagUploadService,
                                                   TypesettingService typesettingService) {
        super(ossTagUploadService);
        this.typesettingService = typesettingService;
    }

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30H_FILM;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        int originalWidth = context.getNestedWidth().intValue();
        int originalHeight = context.getNestedHeight().intValue();
        double expandedHeight = originalHeight + EXPAND_TOP_MM;
        double contentOffsetY = OPEN_BACK_TAG_HEIGHT_MM;

        FormeLayoutBuildResult result = new FormeLayoutBuildResult();
        FormeGenerationRequest.Margin margin = new FormeGenerationRequest.Margin();
        margin.setLeft(0);
        margin.setTop(EXPAND_TOP_MM + OPEN_BACK_TAG_HEIGHT_MM);
        margin.setRight(EXPAND_RIGHT_MM);
        margin.setBottom(OPEN_BACK_TAG_HEIGHT_MM);
        result.setMargin(margin);

        List<Band> orderedBands = CaifuMarkerBands.read(context.getTypesettingInfo());
        boolean hasBloodBand = orderedBands.stream().anyMatch(this::isBloodBand);

        String tagUploadSubDir = buildTagUploadSubDir(context);
        String elementA = ossTagUploadService.uploadTagPng(context.getBusinessId(), createBlackPng(ELEMENT_A_WIDTH_MM, expandedHeight), tagUploadSubDir);
        String elementB = ossTagUploadService.uploadTagPng(context.getBusinessId(), createBlackPng(ELEMENT_B_WIDTH_MM, ELEMENT_B_HEIGHT_MM), tagUploadSubDir);
        String elementD = ossTagUploadService.uploadTagPng(context.getBusinessId(), createBlackPng(ELEMENT_D_WIDTH_TENTH_MM / 10.0, ELEMENT_D_HEIGHT_MM), tagUploadSubDir);
        String elementF = buildOpenBackTagStrip(context, originalWidth, false);
        String elementFRotated = buildOpenBackTagStrip(context, originalWidth, true);

        Map<Integer, String> elementEByHeight = new HashMap<>();
        List<FormeGenerationRequest.Mark> marks = new ArrayList<>();
        marks.add(createMark(elementF, originalWidth, OPEN_BACK_TAG_HEIGHT_MM, 0, 0));
        marks.add(createMark(elementFRotated, originalWidth, OPEN_BACK_TAG_HEIGHT_MM, 0, contentOffsetY + expandedHeight));
        for (Band band : orderedBands) {
            if (band == null) {
                continue;
            }
            double y = band.centerY();
            double elementBY = y + ELEMENT_B_OFFSET_Y_MM;
            if (elementBY <= expandedHeight) {
                marks.add(createMark(elementB, ELEMENT_B_WIDTH_MM, ELEMENT_B_HEIGHT_MM, originalWidth, elementBY + contentOffsetY));
            }
            double lineY = y + ELEMENT_D_OFFSET_Y_MM;
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
                        originalWidth + EXPAND_RIGHT_MM - (ELEMENT_D_OFFSET_RIGHT_ZERO_TENTH_MM / 10.0), lineY + contentOffsetY));
            }
            marks.add(createMark(lineImg, ELEMENT_D_WIDTH_TENTH_MM / 10.0, lineHeight,
                    originalWidth + EXPAND_RIGHT_MM - (ELEMENT_D_OFFSET_RIGHT_ONE_TENTH_MM / 10.0), lineY + contentOffsetY));
            marks.add(createMark(lineImg, ELEMENT_D_WIDTH_TENTH_MM / 10.0, lineHeight,
                    originalWidth + EXPAND_RIGHT_MM - (ELEMENT_D_OFFSET_RIGHT_TWO_TENTH_MM / 10.0), lineY + contentOffsetY));
        }
        marks.add(createMark(elementA, ELEMENT_A_WIDTH_MM, expandedHeight, originalWidth + ELEMENT_A_X_OFFSET_MM, contentOffsetY));

        if (context.getTypesettingInfo() != null) {
            LinkedHashMap<String, String> markFiles = new LinkedHashMap<>();
            markFiles.put("elementA", elementA);
            markFiles.put("elementB", elementB);
            markFiles.put("elementD", elementD);
            markFiles.put("elementF", elementF);
            markFiles.put("elementFRotated", elementFRotated);
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

    /**
     * 按 relatedTypesettingId 直接查询 typesetting，使用 haveBlood 判断是否出血印版。
     */
    private boolean isBloodBand(Band band) {
        if (band == null || StringUtils.isBlank(band.relatedTypesettingId())) {
            return false;
        }
        TypesettingInfo info = typesettingService.findById(band.relatedTypesettingId());
        if (info != null) {
            return Boolean.TRUE.equals(info.getHaveBlood());
        }
        List<TypesettingInfo> infos = typesettingService.findTypesettingListByTypesettingId(band.relatedTypesettingId());
        if (infos != null && !infos.isEmpty() && infos.get(0) != null) {
            return Boolean.TRUE.equals(infos.get(0).getHaveBlood());
        }
        return false;
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

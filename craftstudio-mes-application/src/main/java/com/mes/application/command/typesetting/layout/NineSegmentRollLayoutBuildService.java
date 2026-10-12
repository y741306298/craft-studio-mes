package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

/** 九段卷材标记，所有尺寸和坐标均以 mm 为单位。 */
@Service
public class NineSegmentRollLayoutBuildService extends CaifuLayoutBuildService {
    private static final int EXPAND_LEFT_MM = 12;
    private static final int EXPAND_RIGHT_MM = 14;
    private static final int ELEMENT_A_WIDTH_MM = 10;
    private static final int ELEMENT_A_HEIGHT_MM = 7;
    private static final int ELEMENT_B_WIDTH_MM = 2;
    private static final int ELEMENT_A_OFFSET_Y_MM = 75;

    public NineSegmentRollLayoutBuildService(OssTagUploadService ossTagUploadService) {
        super(ossTagUploadService);
    }

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_NINE_SEGMENT_ROLL;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        BigDecimal height = context.getNestedHeight();
        int svgRightX = EXPAND_LEFT_MM + context.getNestedWidth().setScale(0, java.math.RoundingMode.HALF_UP).intValueExact();
        int rightAX = svgRightX + EXPAND_RIGHT_MM - ELEMENT_A_WIDTH_MM;

        FormeLayoutBuildResult result = new FormeLayoutBuildResult();
        FormeGenerationRequest.Margin margin = new FormeGenerationRequest.Margin();
        margin.setLeft(EXPAND_LEFT_MM);
        margin.setRight(EXPAND_RIGHT_MM);
        margin.setTop(0);
        margin.setBottom(0);
        result.setMargin(margin);

        String subDir = buildTagUploadSubDir(context);
        String elementA = ossTagUploadService.uploadTagPng(context.getBusinessId(),
                createBlackPng(ELEMENT_A_WIDTH_MM, ELEMENT_A_HEIGHT_MM), subDir);
        String elementB = ossTagUploadService.uploadTagPng(context.getBusinessId(),
                createBlackPng(ELEMENT_B_WIDTH_MM, height.setScale(0, java.math.RoundingMode.CEILING).intValueExact()), subDir);

        List<FormeGenerationRequest.Mark> marks = new ArrayList<>();
        FormeGenerationRequest.Mark b = new FormeGenerationRequest.Mark();
        b.setImg(elementB);
        b.setSize(createSize(BigDecimal.valueOf(ELEMENT_B_WIDTH_MM), height));
        // B 的左边缘紧贴原 SVG 右边缘，距扩展后的右边缘 14mm。
        b.setPosition(createPosition(svgRightX, 0));
        marks.add(b);

        // 元素 C 是网格线的 y 坐标集合，额外加入原点并去重。
        LinkedHashSet<Double> ys = new LinkedHashSet<>();
        ys.add(0D);
        TypesettingElement element = context.getTypesettingInfo() == null
                ? null : context.getTypesettingInfo().getElement();
        if (element != null && element.getGridLines() != null && element.getGridLines().getYs() != null) {
            ys.addAll(element.getGridLines().getYs());
        }
        LinkedHashSet<Integer> placedYs = new LinkedHashSet<>();
        for (Double y : ys) {
            if (y == null || !Double.isFinite(y) || y < 0) {
                continue;
            }
            double markY = y + ELEMENT_A_OFFSET_Y_MM;
            if (markY + ELEMENT_A_HEIGHT_MM > height.doubleValue()) {
                continue;
            }
            int positionY = (int) Math.round(markY);
            if (BigDecimal.valueOf((long) positionY + ELEMENT_A_HEIGHT_MM).compareTo(height) > 0
                    || !placedYs.add(positionY)) {
                continue;
            }
            marks.add(createMark(elementA, ELEMENT_A_WIDTH_MM, ELEMENT_A_HEIGHT_MM, 0, positionY));
            marks.add(createMark(elementA, ELEMENT_A_WIDTH_MM, ELEMENT_A_HEIGHT_MM, rightAX, positionY));
        }

        if (context.getTypesettingInfo() != null) {
            LinkedHashMap<String, String> files = new LinkedHashMap<>();
            files.put("elementA", elementA);
            files.put("elementB", elementB);
            context.getTypesettingInfo().setMarks(files);
        }
        result.setMarks(marks);
        result.setAnchorPoints(Collections.emptyList());
        result.setOutputs(buildDefaultOutputs(supportMode(), context));
        result.setUploadPath("forme/" + context.getBusinessId() + "/");
        return result;
    }
}

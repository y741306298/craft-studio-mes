package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.graphics.SvgResourceReader;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

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
        String nestedSvg = context.getTypesettingInfo().getElement().getNestedSvg();
        BoundingBox bounds = readFormeBounds(nestedSvg);

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
        double lbXFromRight = originRightX - (bounds.getMinX().doubleValue() + EXPAND_LEFT_MM);
        double lbYFromTop = bounds.getMaxY().doubleValue() + EXPAND_TOP_MM;
        double cRightBasedX = lbXFromRight + EXPAND_LEFT_MM;
        double cY = lbYFromTop - 500;
        int cWidth = 5;
        if (cRightBasedX + cWidth <= finalWidth && cY >= 0) {
            int cLeftTopX = (int) Math.round(finalWidth - cRightBasedX - cWidth);
            marks.add(createMark(elementC, 5, 10, cLeftTopX, (int) Math.round(cY)));
        }

        // Step7: B based on left-top coordinate (left10, down60 / down80)
        double ltXFromRight = originRightX - (bounds.getMinX().doubleValue() + EXPAND_LEFT_MM);
        double ltYFromTop = bounds.getMinY().doubleValue() + EXPAND_TOP_MM;
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

    private com.mes.application.command.graphics.RemotePng createBlackPng(int width, int height) {
        return com.mes.application.command.graphics.RemoteGraphics.png("BLACK", com.mes.application.command.graphics.RemoteGraphics.params("width", width, "height", height));
    }

    private BoundingBox readFormeBounds(String nestedSvg) {
        if (StringUtils.isBlank(nestedSvg)) {
            throw new IllegalArgumentException("nestedSvg 不能为空");
        }
        try {
            return SvgResourceReader.scanXml(URI.create(nestedSvg), this::parseFormeBoundingBox);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("读取六渡大板标记坐标失败: " + nestedSvg, e);
        }
    }
}

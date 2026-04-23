package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

@Service
public class LiuduSmallGraphLayoutBuildService extends AbstractLayoutModeBuildService {
    /** XY切割-六渡小图模式构建器。 */
    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_LIUDU_SMALL_GRAPH;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        FormeLayoutBuildResult result = new FormeLayoutBuildResult();
        int marginLeft = 100;
        int marginTop = 100;
        int marginRight = 100;
        int marginBottom = 100;
        int elementOriginX = marginLeft;
        int elementOriginY = marginTop;
        FormeGenerationRequest.Margin margin = new FormeGenerationRequest.Margin();
        margin.setLeft(marginLeft);
        margin.setTop(marginTop);
        margin.setRight(marginRight);
        margin.setBottom(marginBottom);
        result.setMargin(margin);

        FormeGenerationRequest.Mark top = new FormeGenerationRequest.Mark();
        top.setImg(context.getBusinessId() + "_top.tif");
        top.setSize(createSize(BigDecimal.valueOf(800), BigDecimal.TEN));
        top.setPosition(createPosition(elementOriginX, 0));
        FormeGenerationRequest.Mark bottom = new FormeGenerationRequest.Mark();
        bottom.setImg(context.getBusinessId() + "_bottom.tif");
        bottom.setSize(createSize(BigDecimal.TEN, BigDecimal.valueOf(1000)));
        bottom.setPosition(createPosition(elementOriginX, elementOriginY + context.getNestedHeight().intValue()));
        result.setMarks(Arrays.asList(top, bottom));

        result.setAnchorPoints(Collections.emptyList());
        result.setOutputs(buildDefaultOutputs(supportMode(), context));
        result.setUploadPath("forme/" + context.getBusinessId() + "/");
        return result;
    }
}

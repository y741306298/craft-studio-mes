package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

@Service
public class CaifuLayoutBuildService extends AbstractLayoutModeBuildService {
    /** XY切割-裁赋模式构建器。 */
    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_A20PR0;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        // 1) margin 与原点定义（单位 mm）
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

        // 2) 标记位于上/下 margin 区域
        FormeGenerationRequest.Mark top = new FormeGenerationRequest.Mark();
        top.setImg(context.getBusinessId() + "_top.tif");
        top.setSize(createSize(BigDecimal.valueOf(800), BigDecimal.TEN));
        top.setPosition(createPosition(elementOriginX, 0));
        FormeGenerationRequest.Mark bottom = new FormeGenerationRequest.Mark();
        bottom.setImg(context.getBusinessId() + "_bottom.tif");
        bottom.setSize(createSize(BigDecimal.TEN, BigDecimal.valueOf(1000)));
        bottom.setPosition(createPosition(elementOriginX, elementOriginY + context.getNestedHeight().intValue()));
        result.setMarks(Arrays.asList(top, bottom));

        // 3) 该模式默认不输出定位点
        result.setAnchorPoints(Collections.emptyList());
        // 4) 输出与上传目录
        result.setOutputs(buildDefaultOutputs(supportMode(), context));
        result.setUploadPath("forme/" + context.getBusinessId() + "/");
        return result;
    }
}

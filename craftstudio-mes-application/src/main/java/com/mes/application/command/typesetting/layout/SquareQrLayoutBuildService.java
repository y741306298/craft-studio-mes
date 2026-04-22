package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class SquareQrLayoutBuildService extends AbstractLayoutModeBuildService {
    /** 方形二维码模式构建器。 */
    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.SHAPED_CUTTING_PLT_QR_SQUARE;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        // 1) 定义 margin 与元素原点（扩展矩形坐标系）
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

        // 2) top/bottom 标记放置在上下 margin 区域
        FormeGenerationRequest.Mark top = new FormeGenerationRequest.Mark();
        top.setImg(context.getBusinessId() + "_top.tif");
        top.setSize(createSize(800, 10));
        top.setPosition(createPosition(elementOriginX, 0));
        FormeGenerationRequest.Mark bottom = new FormeGenerationRequest.Mark();
        bottom.setImg(context.getBusinessId() + "_bottom.tif");
        bottom.setSize(createSize(10, 1000));
        bottom.setPosition(createPosition(elementOriginX, elementOriginY + context.getNestedHeight()));
        result.setMarks(Arrays.asList(top, bottom));

        // 3) 方形定位点示例：位于上 margin 区域的左右两侧
        String anchorSvg = "https://craftstudio-ordering-test.oss-cn-hangzhou.aliyuncs.com/common/anchor/square.svg";
        FormeGenerationRequest.AnchorPoint leftTop = new FormeGenerationRequest.AnchorPoint();
        leftTop.setImg("square.png");
        leftTop.setSvg(anchorSvg);
        leftTop.setSize(createSize(10, 10));
        leftTop.setPosition(createPosition(elementOriginX + 5, marginTop / 2));
        FormeGenerationRequest.AnchorPoint rightTop = new FormeGenerationRequest.AnchorPoint();
        rightTop.setImg("square.png");
        rightTop.setSvg(anchorSvg);
        rightTop.setSize(createSize(10, 10));
        rightTop.setPosition(createPosition(Math.max(elementOriginX + context.getNestedWidth() - 15, elementOriginX + 5), marginTop / 2));
        result.setAnchorPoints(Arrays.asList(leftTop, rightTop));

        // 4) 输出与上传目录
        result.setOutputs(buildDefaultOutputs(supportMode(), context.getBusinessId()));
        result.setUploadPath("forme/" + context.getBusinessId() + "/");
        return result;
    }
}

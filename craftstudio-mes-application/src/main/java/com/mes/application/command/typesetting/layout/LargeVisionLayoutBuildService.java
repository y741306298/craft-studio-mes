package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.springframework.stereotype.Service;

import java.util.Collections;

/** 大视觉排版只输出排版内容，不添加标签、二维码、定位点或额外留边。 */
@Service
public class LargeVisionLayoutBuildService extends AbstractLayoutModeBuildService {

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.LARGE_VISION_LAYOUT;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        FormeLayoutBuildResult result = new FormeLayoutBuildResult();
        FormeGenerationRequest.Margin margin = new FormeGenerationRequest.Margin();
        margin.setLeft(0);
        margin.setTop(0);
        margin.setRight(0);
        margin.setBottom(0);
        result.setMargin(margin);
        result.setMarks(Collections.emptyList());
        result.setAnchorPoints(Collections.emptyList());
        // 不申请二维码队列编号，直接按业务 ID 命名输出文件。
        result.setOutputs(buildDefaultOutputs(supportMode(), context,
                context.getBusinessId() + "-1.plt", context.getBusinessId() + "-2.plt"));
        result.setUploadPath("forme/" + context.getBusinessId() + "/");
        return result;
    }
}

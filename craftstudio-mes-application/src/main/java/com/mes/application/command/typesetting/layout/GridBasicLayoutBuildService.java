package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 基础网格排版构建器：
 * 不增加额外 marks / anchor 放置，仅保留 forme 上传与输出参数拼装。
 */
@Service
public class GridBasicLayoutBuildService extends AbstractLayoutModeBuildService {

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.GRID_TYPESETTING_BASIC;
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
        result.setOutputs(buildDefaultOutputs(supportMode(), context));
        result.setUploadPath("forme/" + context.getBusinessId() + "/");
        return result;
    }
}


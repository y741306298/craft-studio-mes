package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * “净画面”工艺策略。
 *
 * <p>该工艺复用固定尺寸留白的 SVG/PNG 外框生成流程，但外扩尺寸为 0，
 * 只在现有图形同等尺寸位置添加一圈黑色边框，不生成留白标签、不改变生产工件宽高。</p>
 */
@Service
public class JinghuamianProcessStrategy extends AbstractFixedLiubaiProcessStrategy {
    public JinghuamianProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        super(restTemplate, ossTagUploadService);
    }

    @Override
    public boolean matches(LiubaiProcessContext context) {
        if (context == null || context.getProcedureFlow() == null) {
            return false;
        }
        ProcedureFlow procedureFlow = context.getProcedureFlow();
        return !hasNode(procedureFlow, "异形切割") && matchesLiubaiValue(procedureFlow);
    }

    @Override
    protected boolean matchesLiubaiValue(ProcedureFlow procedureFlow) {
        if (procedureFlow.getNodes() == null) {
            return false;
        }
        for (ProcedureFlowNode node : procedureFlow.getNodes()) {
            if (node != null && StringUtils.isNotBlank(node.getNodeName()) && node.getNodeName().contains("净画面")) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected String specName() {
        return "jinghuamian";
    }

    @Override
    protected double expandMm() {
        return 0D;
    }

    @Override
    protected String[] matchKeywords() {
        return new String[]{"净画面"};
    }

    @Override
    protected boolean shouldDrawInnerOriginalBorder() {
        return false;
    }

    @Override
    protected boolean shouldUploadLiubaiTagAssets() {
        return false;
    }

    @Override
    protected boolean shouldInsertMarkGroupsBeforeOriginalContent() {
        return false;
    }

    @Override
    protected String outerRectFillAttributes() {
        return "fill=\"none\"";
    }

    @Override
    protected double borderStrokeWidthScale() {
        return 1.3D;
    }
}

package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import io.micrometer.common.util.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * “包边”工艺策略。
 *
 * <p>命中工艺流中的“包边”节点后，不改变原 mask 尺寸，复用留白 SVG 重构/续写能力，
 * 将“包边”之后的下一个工艺名称按黑、白、黄三色生成 300dpi 标签并贴到原 SVG 四条边上。</p>
 */
@Service
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BaobianProcessStrategy extends AbstractCentimeterLiubaiProcessStrategy {
    public BaobianProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        super(0, restTemplate, ossTagUploadService);
    }

    @Override
    public boolean matches(LiubaiProcessContext context) {
        if (context == null || context.getProcedureFlow() == null) {
            return false;
        }
        ProcedureFlow procedureFlow = context.getProcedureFlow();
        return !hasNode(procedureFlow, "异形切割") && findBaobianNextNodeName(procedureFlow) != null;
    }

    @Override
    protected boolean matchesLiubaiValue(ProcedureFlow procedureFlow) {
        return findBaobianNextNodeName(procedureFlow) != null;
    }

    @Override
    protected String specName() {
        return "baobian";
    }

    @Override
    protected double expandMm() {
        return 0D;
    }

    @Override
    protected String[] matchKeywords() {
        return new String[]{"包边"};
    }

    @Override
    protected boolean shouldDrawInnerOriginalBorder() {
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
    protected String buildLiubaiTagText(LiubaiProcessContext context) {
        if (context == null) {
            return "";
        }
        String nextNodeName = findBaobianNextNodeName(context.getProcedureFlow());
        return StringUtils.isBlank(nextNodeName) ? "" : nextNodeName;
    }

    @Override
    protected List<Color> liubaiTagTextColors() {
        return List.of(Color.BLACK, Color.WHITE, Color.YELLOW);
    }

    private String findBaobianNextNodeName(ProcedureFlow procedureFlow) {
        if (procedureFlow == null || procedureFlow.getNodes() == null || procedureFlow.getNodes().isEmpty()) {
            return null;
        }
        List<ProcedureFlowNode> nodes = orderedNodes(procedureFlow.getNodes());
        for (int index = 0; index < nodes.size(); index++) {
            ProcedureFlowNode node = nodes.get(index);
            if (node == null || StringUtils.isBlank(node.getNodeName()) || !node.getNodeName().contains("包边")) {
                continue;
            }
            for (int nextIndex = index + 1; nextIndex < nodes.size(); nextIndex++) {
                ProcedureFlowNode nextNode = nodes.get(nextIndex);
                if (nextNode != null && StringUtils.isNotBlank(nextNode.getNodeName())) {
                    return nextNode.getNodeName().trim();
                }
            }
            return null;
        }
        return null;
    }

    private List<ProcedureFlowNode> orderedNodes(List<ProcedureFlowNode> nodes) {
        List<ProcedureFlowNode> orderedNodes = new ArrayList<>(nodes);
        orderedNodes.sort(Comparator.comparing(
                ProcedureFlowNode::getNodeOrder,
                Comparator.nullsLast(Integer::compareTo)
        ));
        return orderedNodes;
    }
}

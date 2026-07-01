package com.mes.domain.manufacturer.procedureFlow.util;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;

import java.util.List;

/**
 * 工艺流节点名称匹配工具。
 */
public final class ProcedureFlowNodeMatcher {

    public static final String DOUBLE_SIDE_MOUNTING_KEYWORD = "双面对裱";
    public static final String COVER_DOUBLE_SIDE_KEYWORD = "覆双面";
    public static final String BACK_SIDE_DOUBLE_SIDE_TAPE_KEYWORD = "反面覆双面胶";

    private ProcedureFlowNodeMatcher() {
    }

    public static boolean hasDoubleSideMountingNode(ProcedureFlow procedureFlow) {
        return hasAnyNodeNameContaining(procedureFlow, DOUBLE_SIDE_MOUNTING_KEYWORD, COVER_DOUBLE_SIDE_KEYWORD);
    }

    public static boolean hasAnyNodeNameContaining(ProcedureFlow procedureFlow, String... keywords) {
        if (procedureFlow == null) {
            return false;
        }
        return hasAnyNodeNameContaining(procedureFlow.getNodes(), keywords);
    }

    public static boolean hasAnyNodeNameContaining(List<ProcedureFlowNode> nodes, String... keywords) {
        if (nodes == null || nodes.isEmpty() || keywords == null || keywords.length == 0) {
            return false;
        }
        for (ProcedureFlowNode node : nodes) {
            if (node == null || node.getNodeName() == null || isBackSideDoubleSideTapeNode(node.getNodeName())) {
                continue;
            }
            for (String keyword : keywords) {
                if (keyword != null && !keyword.isBlank() && node.getNodeName().contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isDoubleSideMountingNodeName(String nodeName) {
        if (nodeName == null || isBackSideDoubleSideTapeNode(nodeName)) {
            return false;
        }
        return nodeName.contains(DOUBLE_SIDE_MOUNTING_KEYWORD) || nodeName.contains(COVER_DOUBLE_SIDE_KEYWORD);
    }

    public static boolean containsDoubleSideMountingKeyword(String value) {
        if (value == null) {
            return false;
        }
        String normalizedValue = value.replace(BACK_SIDE_DOUBLE_SIDE_TAPE_KEYWORD, "");
        return normalizedValue.contains(DOUBLE_SIDE_MOUNTING_KEYWORD)
                || normalizedValue.contains(COVER_DOUBLE_SIDE_KEYWORD);
    }

    private static boolean isBackSideDoubleSideTapeNode(String nodeName) {
        return nodeName != null && nodeName.contains(BACK_SIDE_DOUBLE_SIDE_TAPE_KEYWORD);
    }
}

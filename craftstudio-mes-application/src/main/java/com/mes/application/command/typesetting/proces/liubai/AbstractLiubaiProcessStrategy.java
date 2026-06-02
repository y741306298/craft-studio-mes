package com.mes.application.command.typesetting.proces.liubai;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import io.micrometer.common.util.StringUtils;

public abstract class AbstractLiubaiProcessStrategy {

    public boolean matches(LiubaiProcessContext context) {
        if (context == null || context.getProcedureFlow() == null) {
            return false;
        }
        ProcedureFlow procedureFlow = context.getProcedureFlow();
        if (hasNode(procedureFlow, "异形切割") || !hasLiubaiNode(procedureFlow)) {
            return false;
        }
        return matchesLiubaiValue(procedureFlow);
    }

    protected abstract boolean matchesLiubaiValue(ProcedureFlow procedureFlow);

    public abstract void process(LiubaiProcessContext context);

    protected boolean hasLiubaiNode(ProcedureFlow procedureFlow) {
        return procedureFlow.getNodes() != null && procedureFlow.getNodes().stream()
                .anyMatch(node -> node != null && StringUtils.isNotBlank(node.getNodeName()) && node.getNodeName().contains("留白"));
    }

    protected boolean hasNode(ProcedureFlow procedureFlow, String nodeName) {
        return procedureFlow.getNodes() != null && procedureFlow.getNodes().stream()
                .anyMatch(node -> node != null && nodeName.equals(node.getNodeName()));
    }

    protected boolean containsInNodeOrParams(ProcedureFlow procedureFlow, String keyword) {
        if (procedureFlow.getNodes() == null || StringUtils.isBlank(keyword)) {
            return false;
        }
        String normalizedKeyword = normalize(keyword);
        for (ProcedureFlowNode node : procedureFlow.getNodes()) {
            if (node == null || StringUtils.isBlank(node.getNodeName()) || !node.getNodeName().contains("留白")) {
                continue;
            }
            if (normalize(node.getNodeName()).contains(normalizedKeyword)) {
                return true;
            }
            if (node.getParamConfigs() != null && node.getParamConfigs().stream()
                    .anyMatch(config -> config != null && normalize(String.valueOf(config)).contains(normalizedKeyword))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("\\s+", "");
    }
}

package com.mes.application.command.orderPreprocessing.sublimation;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mes.application.command.api.req.RectBloodRequest;
import com.mes.application.command.orderPreprocessing.splice.SpliceProcessStrategies;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.order.orderInfo.entity.OrderItem;

import java.util.ArrayList;
import java.util.List;

/** 读取订单参数并将独立的热升华组合规则适配到矩形旗出血算法。 */
public final class SublimationProcessStrategy {
    public static final String FRONT_CALLBACK = "SUBLIMATION_FRONT";
    public static final String BACK_CALLBACK = "SUBLIMATION_BACK";

    private SublimationProcessStrategy() { }

    public static boolean applies(ProcedureFlow flow) {
        return SublimationRules.applies(steps(flow, false));
    }

    public static SublimationPlan resolve(OrderItem item) {
        if (item == null || !applies(item.getProcedureFlow())) return null;
        ProcedureFlow flow = item.getProcedureFlow();
        boolean unsupported = flow.getNodes().stream().anyMatch(n -> n != null && "异形切割".equals(n.getNodeName()))
                || SpliceProcessStrategies.hasSpliceNode(flow);
        return SublimationRules.resolve(steps(flow, true), unsupported);
    }

    public static RectBloodRequest request(OrderItem item, SublimationPlan plan, boolean back) {
        String url = back ? plan.backImageUrl() : item.getProductionImgFile() == null ? null : item.getProductionImgFile().getRawFile();
        if (url == null || url.isBlank()) throw new IllegalArgumentException("热升华" + (back ? "反面" : "正面") + "图片地址不能为空");
        RectBloodRequest request = new RectBloodRequest();
        request.setImageUrl(url);
        RectBloodRequest.BloodConfig blood = new RectBloodRequest.BloodConfig();
        blood.setScrollEdge(plan.sleeveEdge() == null ? null : RectBloodRequest.ScrollEdge.valueOf(plan.sleeveEdge().name()));
        blood.setScrollWidth(plan.sleeveWidthMm());
        blood.setScrollColorHex(plan.scrollColorHex());
        blood.setSewWidth(plan.sewWidthMm());
        blood.setMirrorRequired(!back && plan.doubleSided() && !plan.differentBack());
        request.setBloodConfig(blood);
        return request;
    }

    private static List<SublimationRules.Step> steps(ProcedureFlow flow, boolean includeParams) {
        if (flow == null || flow.getNodes() == null) return List.of();
        List<SublimationRules.Step> steps = new ArrayList<>();
        for (ProcedureFlowNode node : flow.getNodes()) {
            if (node == null) continue;
            List<JSONObject> params = new ArrayList<>();
            if (includeParams && node.getParamConfigs() != null) {
                for (var config : node.getParamConfigs()) {
                    if (config == null) continue;
                    JSONObject value = JSON.parseObject(JSON.toJSONString(config));
                    if (value.get("param") instanceof java.util.Map<?, ?>) params.add(value.getJSONObject("param"));
                }
            }
            steps.add(new SublimationRules.Step(node.getNodeName(), node.getNodeOrder(), params));
        }
        return steps;
    }
}

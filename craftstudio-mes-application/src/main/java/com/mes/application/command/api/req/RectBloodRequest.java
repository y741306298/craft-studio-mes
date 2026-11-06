package com.mes.application.command.api.req;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mes.application.command.api.vo.CallbackConfig;
import com.mes.application.command.api.vo.UploadConfig;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import lombok.Data;

/** 矩形旗出血请求。 */
@Data
public class RectBloodRequest {
    private String imageUrl;
    private BloodConfig bloodConfig;
    private UploadConfig uploadConfig;
    /** 异步调用时必填。 */
    private CallbackConfig callbackConfig;

    @Data
    public static class BloodConfig {
        private ScrollEdge scrollEdge;
        /** null 或不传时由算法自动采样边缘主色。 */
        private String scrollColorHex;
        private Integer scrollWidth = 20;
        private Integer sewWidth = 10;
        private Boolean mirrorRequired = true;
    }

    public enum ScrollEdge {
        LEFT, TOP, BOTTOM, RIGHT
    }
    /** 只匹配矩形旗缝套筒节点自己的参数；未配置选边类型时无需调用算法。 */
    public static RectBloodRequest fromOrderItem(OrderItem item) {
        if (item == null || item.getProcedureFlow() == null
                || item.getProcedureFlow().getNodes() == null) {
            return null;
        }
        for (ProcedureFlowNode node : item.getProcedureFlow().getNodes()) {
            if (node == null || node.getNodeName() == null || !node.getNodeName().contains("矩形旗缝套筒")
                    || node.getParamConfigs() == null) {
                continue;
            }
            BloodConfig blood = new BloodConfig();
            boolean hasEdgeSelection = false;
            for (var config : node.getParamConfigs()) {
                if (config == null) continue;
                JSONObject value = JSON.parseObject(JSON.toJSONString(config));
                JSONObject param = value.get("param") instanceof java.util.Map<?, ?>
                        ? value.getJSONObject("param") : null;
                // 工艺参数类型统一保存在 param.type。
                String type = param == null ? null : param.getString("type");
                if (!"RECT_EDGE_SELECTION".equals(type) && !"ACC".equals(type)) continue;
                if ("RECT_EDGE_SELECTION".equals(type) && !hasEdgeSelection) {
                    hasEdgeSelection = true;
                    if (param == null || param.get("edges") == null) {
                        throw new IllegalArgumentException("矩形旗缝套筒的 RECT_EDGE_SELECTION 缺少 param.edges");
                    }
                    blood.setScrollEdge(resolveScrollEdge(param.get("edges")));
                }
                if ("ACC".equals(type) && param != null) {
                    JSONObject accessory = param.getJSONObject("accessorySnapshot");
                    if (accessory != null && "大白边".equals(accessory.getString("name"))) {
                        blood.setScrollColorHex("#FFFFFF");
                    }
                }
            }
            if (!hasEdgeSelection) continue;
            if (item.getProductionImgFile() == null || item.getProductionImgFile().getRawFile() == null
                    || item.getProductionImgFile().getRawFile().isBlank()) {
                throw new IllegalArgumentException("矩形旗缝套筒原始图片URL不能为空");
            }
            RectBloodRequest request = new RectBloodRequest();
            request.setImageUrl(item.getProductionImgFile().getRawFile());
            request.setBloodConfig(blood);
            return request;
        }
        return null;
    }
    private static ScrollEdge resolveScrollEdge(Object edges) {
        Object edge = edges;
        if (edges instanceof java.util.Collection<?> selections) {
            if (selections.size() != 1) {
                throw new IllegalArgumentException("矩形旗出血算法要求选择一条缝套边");
            }
            edge = selections.iterator().next();
        }
        try {
            return ScrollEdge.valueOf(String.valueOf(edge).toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("矩形旗缝套筒选边必须为 LEFT/TOP/BOTTOM/RIGHT", e);
        }
    }

}

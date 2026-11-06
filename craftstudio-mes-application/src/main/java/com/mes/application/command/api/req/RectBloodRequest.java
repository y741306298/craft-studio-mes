package com.mes.application.command.api.req;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mes.application.command.api.vo.CallbackConfig;
import com.mes.application.command.api.vo.UploadConfig;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import lombok.Data;

/** 矩形旗缝套筒出血请求。 */
@Data
public class RectBloodRequest {
    private String imageUrl;
    private BloodConfig bloodConfig;
    private UploadConfig uploadConfig;
    private CallbackConfig callbackConfig;

    @Data
    public static class BloodConfig {
        /** 原样传递 RECT_EDGE_SELECTION.param.edges，保留边的选择参数。 */
        private Object scrollEdge;
        private String scrollColorHex;
        /** 单位：毫米，2cm = 20mm。 */
        private double scrollWidth = 20D;
        /** 单位：毫米，1cm = 10mm。 */
        private double sewWidth = 10D;
        private boolean mirrorRequired = true;
    }

    /** 只匹配矩形旗缝套筒节点自己的参数；未配置选边类型时无需调用算法。 */
    public static RectBloodRequest fromOrderItem(OrderItem item) {
        if (item == null || item.getProcedureFlow() == null
                || item.getProcedureFlow().getNodes() == null) {
            return null;
        }
        for (ProcedureFlowNode node : item.getProcedureFlow().getNodes()) {
            if (node == null || !"矩形旗缝套筒".equals(node.getNodeName())
                    || node.getParamConfigs() == null) {
                continue;
            }
            BloodConfig blood = new BloodConfig();
            boolean hasEdgeSelection = false;
            for (var config : node.getParamConfigs()) {
                if (config == null) continue;
                JSONObject value = JSON.parseObject(JSON.toJSONString(config));
                String type = value.getString("type");
                if (!"RECT_EDGE_SELECTION".equals(type) && !"ACC".equals(type)) continue;
                JSONObject param = value.getJSONObject("param");
                if ("RECT_EDGE_SELECTION".equals(type) && !hasEdgeSelection) {
                    hasEdgeSelection = true;
                    if (param == null || param.get("edges") == null) {
                        throw new IllegalArgumentException("矩形旗缝套筒的 RECT_EDGE_SELECTION 缺少 param.edges");
                    }
                    blood.setScrollEdge(param.get("edges"));
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
}

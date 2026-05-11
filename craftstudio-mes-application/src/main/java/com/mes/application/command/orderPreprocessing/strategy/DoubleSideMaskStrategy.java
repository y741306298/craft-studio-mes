package com.mes.application.command.orderPreprocessing.strategy;

import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoubleSideMaskStrategy implements OrderItemProcessingStrategy {

    @Override
    public boolean matches(OrderItem orderItem, ProcedureFlow procedureFlow) {
        return AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "双面对裱")
                || AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "覆双面");
    }

    @Override
    public List<ProductionPiece> process(OrderItem orderItem, ProcedureFlow procedureFlow, AppOrderPreprocessingService processingService) {
        // 步骤1：识别是否同时存在超幅拼接/异形切割，决定是否预先生成等幅蒙版。
        boolean hasCutting = AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "超幅拼接");
        boolean hasSpecialShape = AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "异形切割");
        if (!hasSpecialShape && !hasCutting) {
            // 步骤2：仅双面对裱场景先生成等幅 SVG 并回写到 orderItem.maskImgFile。
            String generatedMaskImgUrl = processingService.generateRectMaskSvgForStrategy(orderItem);
            processingService.saveMaskToOrderItemForStrategy(orderItem, generatedMaskImgUrl);
        }
        // 步骤3：读取反面节点 rawFile 并作为 mirrorUrl 传入算法。
        String mirrorUrl = resolveMirrorRawFile(procedureFlow);
        // 步骤4：异步调用 generateMaskFilesAsync。
        processingService.callMaskAsyncForStrategy(orderItem, procedureFlow, getStrategyType(), hasSpecialShape, hasCutting, mirrorUrl);
        return null;
    }

    /**
     * 双面对裱镜像图提取步骤：
     * 1) 定位“反面相同画面/反面不同画面”节点；
     * 2) 遍历 paramConfigs；
     * 3) 读取 param.file.rawFile；
     * 4) 返回第一个有效 rawFile 作为 mirrorUrl。
     */
    private String resolveMirrorRawFile(ProcedureFlow procedureFlow) {
        if (procedureFlow == null || procedureFlow.getNodes() == null) {
            return null;
        }
        for (com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode node : procedureFlow.getNodes()) {
            if (node == null || node.getNodeName() == null) {
                continue;
            }
            if (!"反面相同画面".equals(node.getNodeName()) && !"反面不同画面".equals(node.getNodeName())) {
                continue;
            }
            if (node.getParamConfigs() == null) {
                continue;
            }
            for (Object config : node.getParamConfigs()) {
                Object param = extractFieldValue(config, "param");
                Object file = extractFieldValue(param, "file");
                Object rawFile = extractFieldValue(file, "rawFile");
                if (rawFile != null && !String.valueOf(rawFile).isBlank()) {
                    return String.valueOf(rawFile);
                }
            }
        }
        return null;
    }

    private Object extractFieldValue(Object target, String fieldName) {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        if (target instanceof java.util.Map<?, ?> map) {
            return map.get(fieldName);
        }
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            return target.getClass().getMethod(getterName).invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public String getStrategyType() {
        return "DOUBLE_SIDE";
    }

    @Override
    public String getStrategyRemark() {
        return "存在双面对裱/覆双面工艺，走双面蒙版异步处理";
    }
}

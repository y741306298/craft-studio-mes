package com.mes.application.command.orderPreprocessing.strategy;

import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Service
public class NoSpecialProcedureStrategy implements OrderItemProcessingStrategy {

    @Override
    public boolean matches(OrderItem orderItem, ProcedureFlow procedureFlow) {
        boolean hasCuttingOrSpecialShape = AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "超幅拼接")
                || AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "异形切割");
        boolean hasDoubleSide = AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "双面对裱")
                || AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "覆双面");
        return !hasCuttingOrSpecialShape && !hasDoubleSide;
    }

    @Override
    public List<ProductionPiece> process(OrderItem orderItem, ProcedureFlow procedureFlow, AppOrderPreprocessingService processingService) {
        // 步骤1：校验工艺参数转化完整性。
        verifyProcedureParamsConverted(procedureFlow, orderItem.getOrderItemId());
        // 步骤2：提取生产图地址与材料宽高。
        String productionImgUrl = orderItem.getProductionImgFile() != null
                && orderItem.getProductionImgFile().getFilePreview() != null
                ? orderItem.getProductionImgFile().getFilePreview().getRaw()
                : null;
        // 步骤3：生成等幅矩形蒙版。
        String generatedMaskImgUrl = processingService.generateRectMaskSvgForStrategy(orderItem);
        Double pieceWidth = extractUsageSizeDimension(orderItem, "getWidth", "getW", "getX");
        Double pieceHeight = extractUsageSizeDimension(orderItem, "getHeight", "getH", "getY");
        // 步骤4：创建并持久化生产零件。
        ProductionPiece piece = processingService.getProcedureService().createProductionPiece(
                orderItem, "ORIGINAL", productionImgUrl, procedureFlow, generatedMaskImgUrl, pieceWidth, pieceHeight);
        processingService.getProductionPieceService().addProductionPiece(piece);
        // 步骤5：写入图搜索引并返回结果。
        processingService.indexProductionPieceImageForStrategy(piece);
        List<ProductionPiece> pieces = new ArrayList<>();
        pieces.add(piece);
        return pieces;
    }

    private Double extractUsageSizeDimension(OrderItem orderItem, String... methodNames) {
        Object usageSize3D = orderItem.getMaterial() == null ? null : orderItem.getMaterial().getUsageSize3D();
        if (usageSize3D == null) {
            return null;
        }
        for (String methodName : methodNames) {
            try {
                Method method = usageSize3D.getClass().getMethod(methodName);
                Object value = method.invoke(usageSize3D);
                if (value instanceof Number number) {
                    return number.doubleValue();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void verifyProcedureParamsConverted(ProcedureFlow procedureFlow, String orderItemId) {
        if (procedureFlow == null || procedureFlow.getNodes() == null) {
            throw new RuntimeException("工艺流程为空，无法生成生产零件：" + orderItemId);
        }
        for (com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode node : procedureFlow.getNodes()) {
            if (node == null || node.getParamConfigs() == null) {
                continue;
            }
            for (Object config : node.getParamConfigs()) {
                if (config == null) {
                    throw new RuntimeException("工艺参数转化失败，存在空参数配置：" + orderItemId);
                }
                Object paramObj = invokeGetter(config, "getParam");
                if (paramObj != null) {
                    continue;
                }
                if (!hasAnyValue(config, "getValue", "getParamValue", "getDefaultValue", "getKey", "getCode", "getName")) {
                    throw new RuntimeException("工艺参数转化失败，参数值为空：" + orderItemId + ", 节点=" + node.getNodeName());
                }
            }
        }
    }

    private boolean hasAnyValue(Object target, String... methodNames) {
        if (target == null) {
            return false;
        }
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);
                if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        Object nestedParam = invokeGetter(target, "getParam");
        return nestedParam != null && hasAnyValue(nestedParam,
                "getProcessParamMetaId", "getType", "getName", "getAccessoryId", "getRawFile", "getValue");
    }

    private Object invokeGetter(Object target, String methodName) {
        if (target == null || StringUtils.isBlank(methodName)) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public String getStrategyType() {
        return "NONE";
    }

    @Override
    public String getStrategyRemark() {
        return "无特殊工艺，直接生成生产零件";
    }
}

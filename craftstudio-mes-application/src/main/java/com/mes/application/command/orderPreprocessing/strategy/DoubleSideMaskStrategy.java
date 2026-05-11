package com.mes.application.command.orderPreprocessing.strategy;

import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.MirrorConfig;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
        MirrorImageData mirrorImageData = resolveMirrorImageData(procedureFlow);
        if (!hasSpecialShape && !hasCutting) {
            // 步骤2：仅双面对裱场景直接按 NoSpecialProcedureStrategy 生成生产零件，不调用算法。
            String generatedMaskImgUrl = processingService.generateRectMaskSvgForStrategy(orderItem);
            processingService.saveMaskToOrderItemForStrategy(orderItem, generatedMaskImgUrl);

            String productionImgUrl = orderItem.getProductionImgFile() != null
                    && orderItem.getProductionImgFile().getFilePreview() != null
                    ? orderItem.getProductionImgFile().getFilePreview().getRaw()
                    : null;
            Double pieceWidth = extractUsageSizeDimension(orderItem, "getWidth", "getW", "getX");
            Double pieceHeight = extractUsageSizeDimension(orderItem, "getHeight", "getH", "getY");
            ProductionPiece piece = processingService.getProcedureService().createProductionPiece(
                    orderItem, "ORIGINAL", productionImgUrl, procedureFlow, generatedMaskImgUrl, pieceWidth, pieceHeight);
            if (mirrorImageData != null && mirrorImageData.raw != null && !mirrorImageData.raw.isBlank()) {
                MirrorConfig mirrorConfig = new MirrorConfig();
                mirrorConfig.setImg(mirrorImageData.raw);
                mirrorConfig.setPreviewImg(mirrorImageData.preview);
                mirrorConfig.setThumbnail(mirrorImageData.thumbnail);
                piece.setMirrorConfigs(List.of(mirrorConfig));
            }
            processingService.getProductionPieceService().addProductionPiece(piece);
            processingService.indexProductionPieceImageForStrategy(piece);
            List<ProductionPiece> pieces = new ArrayList<>();
            pieces.add(piece);
            return pieces;
        }
        // 步骤3：存在超幅拼接/异形切割时才调用异步蒙版算法。
        processingService.callMaskAsyncForDoubleSide(orderItem, procedureFlow, getStrategyType(),
                mirrorImageData == null ? null : mirrorImageData.raw);
        return null;
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

    /**
     * 双面对裱镜像图提取步骤：
     * 1) 定位“反面相同画面/反面不同画面”节点；
     * 2) 遍历 paramConfigs；
     * 3) 读取 param.file.filePreview 下 raw/preview/thumbnail；
     * 4) 返回第一个有效镜像文件信息。
     */
    private MirrorImageData resolveMirrorImageData(ProcedureFlow procedureFlow) {
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
                Object filePreview = extractFieldValue(file, "filePreview");
                String raw = toNonBlankString(extractFieldValue(filePreview, "raw"));
                String preview = toNonBlankString(extractFieldValue(filePreview, "preview"));
                String thumbnail = toNonBlankString(extractFieldValue(filePreview, "thumbnail"));
                if (raw != null) {
                    return new MirrorImageData(raw, preview, thumbnail);
                }
            }
        }
        return null;
    }

    private String toNonBlankString(Object value) {
        if (value == null) {
            return null;
        }
        String str = String.valueOf(value);
        return str.isBlank() ? null : str;
    }

    private static class MirrorImageData {
        private final String raw;
        private final String preview;
        private final String thumbnail;

        private MirrorImageData(String raw, String preview, String thumbnail) {
            this.raw = raw;
            this.preview = preview;
            this.thumbnail = thumbnail;
        }
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

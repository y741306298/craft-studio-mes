package com.mes.application.command.orderPreprocessing.strategy;

import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.application.command.orderPreprocessing.splice.SpliceProcessStrategies;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.MirrorConfig;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Service
public class DoubleSideMaskStrategy implements OrderItemProcessingStrategy {

    @Autowired
    private OrderInfoService orderInfoService;

    @Override
    public boolean matches(OrderItem orderItem, ProcedureFlow procedureFlow) {
        return AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "双面对裱")
                || AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "覆双面");
    }

    @Override
    public List<ProductionPiece> process(OrderItem orderItem, ProcedureFlow procedureFlow, AppOrderPreprocessingService processingService) {
        // 步骤1：识别是否同时存在拼接/异形切割，决定是否预先生成等幅蒙版。
        boolean hasSplicing = SpliceProcessStrategies.hasSpliceNode(procedureFlow);
        boolean hasSpecialShape = AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "异形切割");
        MirrorImageData mirrorImageData = resolveMirrorImageData(procedureFlow, orderItem);
        if (!hasSpecialShape && !hasSplicing) {
            // 步骤2：仅双面对裱场景直接按 NoSpecialProcedureStrategy 生成生产零件，不调用算法。
            String generatedMaskImgUrl = processingService.generateRectMaskSvgForStrategy(orderItem);
            processingService.saveMaskToOrderItemForStrategy(orderItem, generatedMaskImgUrl);

            String productionImgUrl = orderItem.getProductionImgFile() != null
                    && orderItem.getProductionImgFile().getFilePreview() != null
                    ? orderItem.getProductionImgFile().getFilePreview().getRaw()
                    : null;
            Double pieceWidth = toMillimeters(extractUsageSizeDimension(orderItem, "getWidth", "getW", "getX"));
            Double pieceHeight = toMillimeters(extractUsageSizeDimension(orderItem, "getHeight", "getH", "getY"));
            ProductionPiece piece = processingService.getProcedureService().createProductionPiece(
                    orderItem, "ORIGINAL", productionImgUrl, procedureFlow, generatedMaskImgUrl, pieceWidth, pieceHeight);
            
            OrderInfo orderInfo = orderInfoService.findByOrderId(orderItem.getOrderId());
            if (orderInfo != null && StringUtils.isNotBlank(orderInfo.getRemark())) {
                piece.setRemark(orderInfo.getRemark());
            }
            
            if (mirrorImageData != null && mirrorImageData.raw != null && !mirrorImageData.raw.isBlank()) {
                MirrorConfig mirrorConfig = new MirrorConfig();
                mirrorConfig.setImg(processingService.completeOssUrlForStrategy(mirrorImageData.raw));
                mirrorConfig.setSvg(processingService.completeOssUrlForStrategy(mirrorImageData.raw));
                mirrorConfig.setPreviewImg(processingService.completeOssUrlForStrategy(mirrorImageData.preview));
                mirrorConfig.setThumbnail(processingService.completeOssUrlForStrategy(mirrorImageData.thumbnail));
                piece.setMirrorConfigs(List.of(mirrorConfig));
            }
            processingService.getProductionPieceService().addProductionPiece(piece);
            processingService.indexProductionPieceImageForStrategy(piece);
            List<ProductionPiece> pieces = new ArrayList<>();
            pieces.add(piece);
            return pieces;
        }
        // 步骤3：存在拼接/异形切割时才调用异步蒙版算法。
        processingService.callMaskAsyncForDoubleSide(orderItem, procedureFlow, getStrategyType(),
                mirrorImageData == null ? null : mirrorImageData.raw);
        return null;
    }

    private Double toMillimeters(Double centimeters) {
        return centimeters == null ? null : centimeters * 10;
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
     * 2) 优先遍历 paramConfigs，读取 param.file.filePreview 下 raw/preview/thumbnail；
     * 3) “反面相同画面”允许无参数，此时使用订单项生产图 filePreview 作为镜像文件信息；
     * 4) 返回第一个有效镜像文件信息。
     */
    private MirrorImageData resolveMirrorImageData(ProcedureFlow procedureFlow, OrderItem orderItem) {
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
            MirrorImageData paramMirrorImageData = resolveMirrorImageDataFromNodeParams(node);
            if (paramMirrorImageData != null) {
                return paramMirrorImageData;
            }
            if ("反面相同画面".equals(node.getNodeName())) {
                return resolveMirrorImageDataFromProductionImage(orderItem);
            }
        }
        return null;
    }

    private MirrorImageData resolveMirrorImageDataFromNodeParams(com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode node) {
        if (node == null || node.getParamConfigs() == null) {
            return null;
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
        return null;
    }

    private MirrorImageData resolveMirrorImageDataFromProductionImage(OrderItem orderItem) {
        if (orderItem == null || orderItem.getProductionImgFile() == null
                || orderItem.getProductionImgFile().getFilePreview() == null) {
            return null;
        }
        String raw = toNonBlankString(orderItem.getProductionImgFile().getFilePreview().getRaw());
        String preview = toNonBlankString(orderItem.getProductionImgFile().getFilePreview().getPreview());
        String thumbnail = toNonBlankString(orderItem.getProductionImgFile().getFilePreview().getThumbnail());
        return raw == null ? null : new MirrorImageData(raw, preview, thumbnail);
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

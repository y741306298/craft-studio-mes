package com.mes.application.command.typesetting.strategy.policy;

import com.mes.application.command.api.req.NestingRequest;
import com.mes.application.command.orderPreprocessing.sublimation.SublimationSvg;
import com.mes.domain.manufacturer.procedureFlow.util.ProcedureFlowNodeMatcher;
import com.mes.domain.manufacturer.productionPiece.entity.MirrorConfig;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 为双面对裱及带反面画面的热升华零件匹配算法反面配置。
 */
@Component
public class DoubleSideMountingManifestPolicy implements NestingManifestPolicy {

    private static final String MARKED_NESTING_ID_PREFIX = "marked-nesting-";

    @Override
    public boolean matches(List<ProductionPiece> productionPieces, List<TypesettingInfo> typesettingInfos) {
        return hasDoubleSideMounting(productionPieces);
    }

    @Override
    public void apply(NestingRequest.NestManifest nestManifest,
                      List<ProductionPiece> productionPieces,
                      List<TypesettingInfo> typesettingInfos) {
        if (nestManifest == null) {
            return;
        }
        fillMirrorConfigForElements(nestManifest, productionPieces);
    }

    private void fillMirrorConfigForElements(NestingRequest.NestManifest nestManifest,
                                             List<ProductionPiece> productionPieces) {
        if (nestManifest.getElements() == null || productionPieces == null) {
            return;
        }
        for (NestingRequest.Element element : nestManifest.getElements()) {
            if (element == null || StringUtils.isBlank(element.getId())) {
                continue;
            }
            ProductionPiece piece = findPieceById(productionPieces, element.getId());
            if (piece != null && ProcedureFlowNodeMatcher.hasSublimationNode(piece.getProcedureFlow())
                    && !ProcedureFlowNodeMatcher.hasSublimationBackImageNode(piece.getProcedureFlow())) {
                continue;
            }
            boolean sublimationBack = piece != null
                    && ProcedureFlowNodeMatcher.hasSublimationBackImageNode(piece.getProcedureFlow());
            if (piece == null || piece.getMirrorConfigs() == null || piece.getMirrorConfigs().isEmpty()) {
                if (sublimationBack) throw new IllegalArgumentException("热升华零件缺少反面文件：" + piece.getId());
                continue;
            }
            MirrorConfig mirrorConfig = piece.getMirrorConfigs().get(0);
            if (mirrorConfig != null && StringUtils.isNotBlank(mirrorConfig.getImg())) {
                NestingRequest.ElementMirrorConfig elementMirrorConfig = new NestingRequest.ElementMirrorConfig();
                elementMirrorConfig.setImg(mirrorConfig.getImg());
                Map<String, NestingRequest.ElementMirrorConfig> elementMirrorConfigs = new LinkedHashMap<>();
                // marked 元素按整体 SVG 解析，算法用 key 查找 SVG 内的具体画面分组，
                // 不是外层元素 ID，也不是去掉 marked-nesting- 后的数据库 ID。
                String targetId = sublimationBack && element.getId().startsWith(MARKED_NESTING_ID_PREFIX)
                        ? SublimationSvg.originalGroupId(piece.getId()) : piece.getId();
                elementMirrorConfigs.put(targetId, elementMirrorConfig);
                element.setMirrorConfig(elementMirrorConfigs);
            } else if (sublimationBack) {
                throw new IllegalArgumentException("热升华零件缺少反面图片地址：" + piece.getId());
            }
        }
    }

    private ProductionPiece findPieceById(List<ProductionPiece> productionPieces, String elementId) {
        String pieceId = normalizeElementPieceId(elementId);
        for (ProductionPiece piece : productionPieces) {
            if (piece != null && pieceId.equals(piece.getId())) {
                return piece;
            }
        }
        return null;
    }

    /**
     * 带 marks 的工件在排版请求中会使用 marked-nesting-{pieceId} 作为算法元素 ID，
     * 避免回调解析 nestedSvg 时按外层和内层相同 ID 重复计数。
     * 先还原数据库 ID 定位生产工件；算法 mirrorConfig 的 key 再按 SVG 内画面分组单独设置。
     */
    private String normalizeElementPieceId(String elementId) {
        if (StringUtils.isBlank(elementId)) {
            return elementId;
        }
        if (elementId.startsWith(MARKED_NESTING_ID_PREFIX)) {
            return elementId.substring(MARKED_NESTING_ID_PREFIX.length());
        }
        return elementId;
    }

    private boolean hasDoubleSideMounting(List<ProductionPiece> productionPieces) {
        if (productionPieces != null) {
            for (ProductionPiece piece : productionPieces) {
                if (piece != null && (ProcedureFlowNodeMatcher.hasDoubleSideMountingNode(piece.getProcedureFlow())
                        || ProcedureFlowNodeMatcher.hasSublimationBackImageNode(piece.getProcedureFlow()))) {
                    return true;
                }
            }
        }
        return false;
    }

}

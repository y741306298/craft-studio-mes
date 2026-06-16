package com.mes.application.command.typesetting.strategy.policy;

import com.mes.application.command.api.req.NestingRequest;
import com.mes.domain.manufacturer.procedureFlow.util.ProcedureFlowNodeMatcher;
import com.mes.domain.manufacturer.productionPiece.entity.MirrorConfig;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * “双面对裱”策略：存在该工艺时，镜像附加且镜像不要求 plt。
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
        nestManifest.setMirrorAppend(Boolean.TRUE);
        nestManifest.setMirrorRequirePlt(Boolean.FALSE);
        fillMirrorImgForElements(nestManifest, productionPieces);
    }

    private void fillMirrorImgForElements(NestingRequest.NestManifest nestManifest,
                                          List<ProductionPiece> productionPieces) {
        if (nestManifest.getElements() == null || productionPieces == null) {
            return;
        }
        for (NestingRequest.Element element : nestManifest.getElements()) {
            if (element == null || StringUtils.isBlank(element.getId())) {
                continue;
            }
            ProductionPiece piece = findPieceById(productionPieces, element.getId());
            if (piece == null || piece.getMirrorConfigs() == null || piece.getMirrorConfigs().isEmpty()) {
                continue;
            }
            MirrorConfig mirrorConfig = piece.getMirrorConfigs().get(0);
            if (mirrorConfig != null && StringUtils.isNotBlank(mirrorConfig.getImg())) {
                element.setMirrorImg(mirrorConfig.getImg());
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
     * 双面对裱镜像图仍需要按原始生产工件 ID 回填，因此这里统一还原。
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
                if (piece != null && ProcedureFlowNodeMatcher.hasDoubleSideMountingNode(piece.getProcedureFlow())) {
                    return true;
                }
            }
        }
        return false;
    }

}

package com.mes.application.command.typesetting.strategy.policy;

import com.mes.application.command.api.req.NestingRequest;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
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

    private static final String DOUBLE_SIDE_NODE_NAME = "双面对裱";

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

    private ProductionPiece findPieceById(List<ProductionPiece> productionPieces, String pieceId) {
        for (ProductionPiece piece : productionPieces) {
            if (piece != null && pieceId.equals(piece.getId())) {
                return piece;
            }
        }
        return null;
    }

    private boolean hasDoubleSideMounting(List<ProductionPiece> productionPieces) {
        if (productionPieces != null) {
            for (ProductionPiece piece : productionPieces) {
                if (piece != null && procedureFlowHasNode(piece.getProcedureFlow(), DOUBLE_SIDE_NODE_NAME)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean procedureFlowHasNode(ProcedureFlow procedureFlow, String nodeName) {
        if (procedureFlow == null || procedureFlow.getNodes() == null || StringUtils.isBlank(nodeName)) {
            return false;
        }
        for (ProcedureFlowNode node : procedureFlow.getNodes()) {
            if (node != null && nodeName.equals(node.getNodeName())) {
                return true;
            }
        }
        return false;
    }
}

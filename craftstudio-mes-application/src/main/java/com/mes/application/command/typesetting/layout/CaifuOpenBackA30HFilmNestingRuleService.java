package com.mes.application.command.typesetting.layout;

import com.mes.application.command.typesetting.api.req.LayoutConfirmRequest;
import com.mes.application.command.api.req.NestingRequest;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.order.entity.ProductionPiece;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CaifuOpenBackA30HFilmNestingRuleService implements NestingRequestRuleService {

    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30H_FILM;
    }

    @Override
    public void validateBeforeBuild(LayoutConfirmRequest request,
                                    List<ProductionPiece> productionPieces,
                                    List<TypesettingInfo> typesettingInfos) {
        double containerMaxWidth = resolveMaxContainerWidth(request);
        for (ProductionPiece piece : productionPieces) {
            if (piece == null || piece.getWidth() == null) {
                continue;
            }
            if (piece.getWidth() > containerMaxWidth) {
                String pieceId = StringUtils.isNotBlank(piece.getProductionPieceId()) ? piece.getProductionPieceId() : piece.getId();
                throw new IllegalArgumentException("零件" + pieceId + "的宽度超过限定宽度，无法排版");
            }
        }
        for (TypesettingInfo info : typesettingInfos) {
            if (info == null || info.getElement() == null || info.getElement().getWidth() == null) {
                continue;
            }
            if (info.getElement().getWidth().doubleValue() > containerMaxWidth) {
                String pieceId = StringUtils.isNotBlank(info.getTypesettingId()) ? info.getTypesettingId() : info.getId();
                throw new IllegalArgumentException("零件" + pieceId + "的宽度超过限定宽度，无法排版");
            }
        }
    }

    @Override
    public void applyElementStyle(NestingRequest.Element element, boolean isBloodElement) {
        element.setHGravity("right");
        element.setVMargin(0);
        element.setHMargin(isBloodElement ? 0 : 30);
    }

    private double resolveMaxContainerWidth(LayoutConfirmRequest request) {
        double maxWidth = 1500D;
        if (request.getContainers() == null) {
            return maxWidth;
        }
        for (LayoutConfirmRequest.ContainerInfo containerInfo : request.getContainers()) {
            if (containerInfo == null || containerInfo.getWidth() == null) {
                continue;
            }
            maxWidth = Math.max(maxWidth, containerInfo.getWidth().doubleValue());
        }
        return maxWidth;
    }
}

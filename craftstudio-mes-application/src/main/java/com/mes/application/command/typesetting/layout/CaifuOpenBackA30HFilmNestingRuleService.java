package com.mes.application.command.typesetting.layout;

import com.mes.application.dto.req.typesetting.LayoutConfirmRequest;
import com.mes.application.command.api.req.NestingRequest;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
        element.setHMargin(0);
    }

    @Override
    public void arrangeElementSources(List<ProductionPiece> productionPieces,
                                      List<TypesettingInfo> typesettingInfos) {
        groupConsecutively(productionPieces, ProductionPiece::getOrderItemId);
        groupConsecutively(typesettingInfos, TypesettingInfo::getTypesettingId);
    }

    /**
     * Keeps the first-seen group order while making every non-blank group contiguous.
     * Blank identifiers deliberately receive independent keys and therefore retain
     * their relative positions instead of being treated as one business group.
     */
    private <T> void groupConsecutively(List<T> sources, Function<T, String> groupIdResolver) {
        if (sources == null || sources.size() < 2) {
            return;
        }
        Map<Object, List<T>> groups = new LinkedHashMap<>();
        for (T source : sources) {
            String groupId = source == null ? null : groupIdResolver.apply(source);
            Object groupKey = StringUtils.isBlank(groupId) ? new Object() : groupId;
            groups.computeIfAbsent(groupKey, ignored -> new ArrayList<>()).add(source);
        }
        sources.clear();
        groups.values().forEach(sources::addAll);
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

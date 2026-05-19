package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.NestingRequest;
import com.mes.application.command.typesetting.api.req.LayoutConfirmRequest;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.order.entity.ProductionPiece;

import java.util.List;

/**
 * buildNestingRequest 阶段的可扩展规则扩展点。
 */
public interface NestingRequestRuleService {

    TypesettingLayoutMode supportMode();

    default void validateBeforeBuild(LayoutConfirmRequest request,
                                     List<ProductionPiece> productionPieces,
                                     List<TypesettingInfo> typesettingInfos) {
    }

    default void applyElementStyle(NestingRequest.Element element, boolean isBloodElement) {
    }
}

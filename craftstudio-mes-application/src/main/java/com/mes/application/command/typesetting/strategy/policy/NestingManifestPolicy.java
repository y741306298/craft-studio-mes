package com.mes.application.command.typesetting.strategy.policy;

import com.mes.application.command.api.req.NestingRequest;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;

import java.util.List;

/**
 * NestManifest 策略抽象：
 * <ul>
 *   <li>matches: 判断当前上下文是否命中该策略；</li>
 *   <li>apply: 对 NestManifest 做策略回填。</li>
 * </ul>
 */
public interface NestingManifestPolicy {

    boolean matches(List<ProductionPiece> productionPieces, List<TypesettingInfo> typesettingInfos);

    void apply(NestingRequest.NestManifest nestManifest,
               List<ProductionPiece> productionPieces,
               List<TypesettingInfo> typesettingInfos);
}

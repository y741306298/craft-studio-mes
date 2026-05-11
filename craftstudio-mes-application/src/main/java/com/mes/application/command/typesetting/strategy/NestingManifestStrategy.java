package com.mes.application.command.typesetting.strategy;

import com.mes.application.command.api.req.NestingRequest;
import com.mes.application.command.typesetting.strategy.policy.NestingManifestPolicy;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * NestManifest 策略执行器：根据策略匹配结果决定执行哪些策略。
 */
@Component
public class NestingManifestStrategy {

    @Autowired(required = false)
    private List<NestingManifestPolicy> policies;

    public void apply(NestingRequest.NestManifest nestManifest,
                      List<ProductionPiece> productionPieces,
                      List<TypesettingInfo> typesettingInfos) {
        if (nestManifest == null) {
            return;
        }
        List<NestingManifestPolicy> effectivePolicies = policies == null ? Collections.emptyList() : policies;
        for (NestingManifestPolicy policy : effectivePolicies) {
            if (policy == null) {
                continue;
            }
            if (policy.matches(productionPieces, typesettingInfos)) {
                policy.apply(nestManifest, productionPieces, typesettingInfos);
            }
        }
    }
}

package com.mes.application.command.typesetting.strategy;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;

public interface SpecialCraftMarkStrategy {

    void apply(TypesettingInfo typesettingInfo, FormeGenerationRequest formeRequest);
}

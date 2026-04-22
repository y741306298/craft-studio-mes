package com.mes.application.command.typesetting.layout;

import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;

public interface TypesettingLayoutModeBuildService {
    TypesettingLayoutMode supportMode();

    FormeLayoutBuildResult build(FormeBuildContext context);
}

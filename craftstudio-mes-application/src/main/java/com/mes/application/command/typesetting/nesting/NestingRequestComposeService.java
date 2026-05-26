package com.mes.application.command.typesetting.nesting;

import com.mes.application.command.api.req.NestingRequest;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;

import java.util.List;

public interface NestingRequestComposeService {

    TypesettingLayoutMode supportMode();

    default int resolveSpacing(TypesettingLayoutMode layoutMode) {
        return layoutMode.getNestingSpacingMm();
    }

    default List<NestingRequest.Element> composeElements(String manufacturerMetaId,
                                                         String businessId,
                                                         List<NestingRequest.Element> elements,
                                                         List<NestingRequest.Container> containers) {
        return elements;
    }
}

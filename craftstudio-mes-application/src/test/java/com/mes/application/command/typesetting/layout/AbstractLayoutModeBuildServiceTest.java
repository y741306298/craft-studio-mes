package com.mes.application.command.typesetting.layout;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractLayoutModeBuildServiceTest {
    private final TestLayoutBuildService service = new TestLayoutBuildService();

    @Test
    void shouldUseSixMillimeterAnchorForConfiguredManufacturer() {
        FormeBuildContext context = contextWithManufacturer("69f95b080ff1ad90a9611468");

        assertEquals(6, service.anchorSize(context, 4));
    }

    @Test
    void shouldKeepDefaultAnchorSizeForOtherManufacturer() {
        FormeBuildContext context = contextWithManufacturer("another-manufacturer");

        assertEquals(4, service.anchorSize(context, 4));
    }

    private FormeBuildContext contextWithManufacturer(String manufacturerMetaId) {
        TypesettingInfo typesettingInfo = new TypesettingInfo();
        typesettingInfo.setManufacturerMetaId(manufacturerMetaId);
        FormeBuildContext context = new FormeBuildContext();
        context.setTypesettingInfo(typesettingInfo);
        return context;
    }

    private static class TestLayoutBuildService extends AbstractLayoutModeBuildService {
        private int anchorSize(FormeBuildContext context, int defaultSizeMm) {
            return resolveAnchorSizeMm(context, defaultSizeMm);
        }

        @Override
        public TypesettingLayoutMode supportMode() {
            return null;
        }

        @Override
        public FormeLayoutBuildResult build(FormeBuildContext context) {
            return null;
        }
    }
}

package com.mes.application.command.typesetting;

import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppTypesettingServiceCallbackSourceCellTest {

    @Test
    void singleResultNeverReplacesPersistedSourceCells() {
        AppTypesettingService service = new AppTypesettingService();

        Boolean shouldUpdate = ReflectionTestUtils.invokeMethod(service,
                "shouldUpdateCallbackSourceCells", 1, List.of(new TypesettingSourceCell()));

        assertThat(shouldUpdate).isFalse();
    }

    @Test
    void failedSourceResolutionDoesNotReplacePersistedSourceCells() {
        AppTypesettingService service = new AppTypesettingService();

        Boolean shouldUpdate = ReflectionTestUtils.invokeMethod(service,
                "shouldUpdateCallbackSourceCells", 2, List.of());

        assertThat(shouldUpdate).isFalse();
    }

    @Test
    void successfullyResolvedMultiResultMayReplaceSourceCells() {
        AppTypesettingService service = new AppTypesettingService();

        Boolean shouldUpdate = ReflectionTestUtils.invokeMethod(service,
                "shouldUpdateCallbackSourceCells", 2, List.of(new TypesettingSourceCell()));

        assertThat(shouldUpdate).isTrue();
    }
}

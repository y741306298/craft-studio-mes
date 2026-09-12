package com.mes.application.command.typesetting;

import com.mes.application.command.typesetting.vo.RetryFormeGenerationResult;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppTypesettingServiceRetryFormeTest {

    @Test
    void scansAllConfirmedTypesettingsAndReturnsEmptySummary() {
        AppTypesettingService service = new AppTypesettingService();
        TypesettingService typesettingService = mock(TypesettingService.class);
        ReflectionTestUtils.setField(service, "domainTypesettingService", typesettingService);
        when(typesettingService.findAllByStatus(TypesettingStatus.CONFIRMED))
                .thenReturn(Collections.emptyList());

        RetryFormeGenerationResult result = service.retryAllConfirmedFormeGeneration();

        assertThat(result.getTotal()).isZero();
        assertThat(result.getSubmitted()).isZero();
        assertThat(result.getSkipped()).isZero();
        assertThat(result.getFailed()).isZero();
        assertThat(result.getFailures()).isEmpty();
        verify(typesettingService).findAllByStatus(TypesettingStatus.CONFIRMED);
    }
}

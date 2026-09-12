package com.mes.application.command.typesetting;

import com.mes.application.command.typesetting.vo.RetryFormeGenerationResult;
import com.mes.application.dto.req.typesetting.RetryConfirmedFormeRequest;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppTypesettingServiceRetryFormeTest {

    @Test
    void rejectsMissingDateRangeRequest() {
        AppTypesettingService service = new AppTypesettingService();

        assertThatThrownBy(() -> service.retryAllConfirmedFormeGeneration(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("重试印版生成参数不能为空");
    }

    @Test
    void scansAllConfirmedTypesettingsAndReturnsEmptySummary() {
        AppTypesettingService service = new AppTypesettingService();
        TypesettingService typesettingService = mock(TypesettingService.class);
        ReflectionTestUtils.setField(service, "domainTypesettingService", typesettingService);
        Date startTime = new Date(1_000L);
        Date endTime = new Date(2_000L);
        when(typesettingService.findAllByStatusAndCreateTime(TypesettingStatus.CONFIRMED, startTime, endTime))
                .thenReturn(Collections.emptyList());
        RetryConfirmedFormeRequest request = new RetryConfirmedFormeRequest();
        request.setStartTime(startTime);
        request.setEndTime(endTime);

        RetryFormeGenerationResult result = service.retryAllConfirmedFormeGeneration(request);

        assertThat(result.getTotal()).isZero();
        assertThat(result.getSubmitted()).isZero();
        assertThat(result.getSkipped()).isZero();
        assertThat(result.getFailed()).isZero();
        assertThat(result.getFailures()).isEmpty();
        verify(typesettingService).findAllByStatusAndCreateTime(TypesettingStatus.CONFIRMED, startTime, endTime);
    }
}

package com.mes.application.command.typesetting;

import com.mes.application.command.api.AlgorithmCoreApiService;
import com.mes.application.command.api.req.NestingRequest;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class AppTypesettingServiceSubmissionTest {

    @Test
    void persistsTypesettingBeforeSubmittingAsyncAlgorithmRequest() {
        TypesettingService typesettingService = mock(TypesettingService.class);
        AlgorithmCoreApiService algorithmCoreApiService = mock(AlgorithmCoreApiService.class);
        AppTypesettingService service = new AppTypesettingService();
        ReflectionTestUtils.setField(service, "domainTypesettingService", typesettingService);
        ReflectionTestUtils.setField(service, "algorithmCoreApiService", algorithmCoreApiService);
        TypesettingInfo typesettingInfo = new TypesettingInfo();
        NestingRequest nestingRequest = new NestingRequest();

        service.persistAndSubmitTypesetting(typesettingInfo, nestingRequest,
                TypesettingLayoutMode.RECT_TYPESETTING_BASIC);

        InOrder inOrder = inOrder(typesettingService, algorithmCoreApiService);
        inOrder.verify(typesettingService).addTypesetting(typesettingInfo);
        inOrder.verify(algorithmCoreApiService).generateRectNestedFilesAsync(nestingRequest);
    }
}

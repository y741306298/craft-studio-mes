package com.mes.application.command.api;

import com.mes.application.command.api.vo.CallbackConfig;
import com.mes.application.command.api.vo.CallbackCustomValue;
import com.mes.domain.shared.algorithm.entity.AlgorithmCoreApiCallRecord;
import com.mes.domain.shared.algorithm.repository.AlgorithmCoreApiCallRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AlgorithmCoreApiServiceTest {

    private AlgorithmCoreApiService service;
    private AlgorithmCoreApiCallRecordRepository repository;

    @BeforeEach
    void setUp() {
        service = new AlgorithmCoreApiService();
        repository = mock(AlgorithmCoreApiCallRecordRepository.class);
        ReflectionTestUtils.setField(service, "algorithmCoreApiCallRecordRepository", repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "generateMaskFilesAsync",
            "generateMaskFilesSync",
            "convertGrayImgToSvgAsync",
            "convertGrayImgToSvg"
    })
    void saveCallRecordUsesOrderItemIdForPreprocessingTypes(String type) {
        CallbackCustomValue customValue = new CallbackCustomValue();
        customValue.setId("order-item-1:request-1");
        customValue.setOrderItemId("order-item-1");

        saveCallRecord(type, customValue);

        assertEquals("order-item-1", captureRecord().getSourceId());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "generateNestedFilesAsync",
            "generateNestedFilesSync",
            "generateGridNestedFilesAsync",
            "generateRectNestedFilesAsync",
            "generateVerticalNestedFilesAsync",
            "generateFormeAsync",
            "generateForme"
    })
    void saveCallRecordUsesCallbackIdForTypesettingTypes(String type) {
        CallbackCustomValue customValue = new CallbackCustomValue();
        customValue.setId("typesetting-1");

        saveCallRecord(type, customValue);

        assertEquals("typesetting-1", captureRecord().getSourceId());
    }

    @Test
    void saveCallRecordLeavesSourceIdEmptyWhenTypeHasNoBusinessSource() {
        CallbackCustomValue customValue = new CallbackCustomValue();
        customValue.setId("typesetting-1");

        saveCallRecord("convertSvgToPlt", customValue);

        assertNull(captureRecord().getSourceId());
    }

    private void saveCallRecord(String type, CallbackCustomValue customValue) {
        TestRequest request = new TestRequest(customValue);
        ReflectionTestUtils.invokeMethod(service, "saveCallRecord", "async", "url", "", request, type);
    }

    private AlgorithmCoreApiCallRecord captureRecord() {
        ArgumentCaptor<AlgorithmCoreApiCallRecord> captor = ArgumentCaptor.forClass(AlgorithmCoreApiCallRecord.class);
        verify(repository).add(captor.capture());
        return captor.getValue();
    }

    public static class TestRequest {
        private final CallbackConfig callbackConfig;

        private TestRequest(CallbackCustomValue callbackCustomValue) {
            callbackConfig = new CallbackConfig();
            callbackConfig.setCallbackCustomValue(callbackCustomValue);
        }

        public CallbackConfig getCallbackConfig() {
            return callbackConfig;
        }
    }
}

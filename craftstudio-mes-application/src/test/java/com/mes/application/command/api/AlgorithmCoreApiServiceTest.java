package com.mes.application.command.api;

import com.alibaba.fastjson2.JSONObject;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

    @Test
    void saveCallRecordStoresCallbackCustomValueAsJsonObject() {
        CallbackCustomValue customValue = new CallbackCustomValue();
        customValue.setId("LAYoUT202609072220531364");
        CallbackConfig callbackConfig = new CallbackConfig();
        callbackConfig.setCallbackCustomValue(customValue);

        ReflectionTestUtils.invokeMethod(
                service,
                "saveCallRecord",
                "async",
                "url",
                "apiPath",
                new TestRequest(callbackConfig),
                "generateNestedFilesAsync");

        ArgumentCaptor<AlgorithmCoreApiCallRecord> captor =
                ArgumentCaptor.forClass(AlgorithmCoreApiCallRecord.class);
        verify(repository).add(captor.capture());
        Object storedValue = captor.getValue().getCallbackCustomValue();
        JSONObject storedJson = assertInstanceOf(JSONObject.class, storedValue);
        assertEquals("LAYoUT202609072220531364", storedJson.getString("id"));
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
    void saveCallRecordUsesCallbackIdAsTypesettingSourceId(String type) {
        CallbackCustomValue customValue = new CallbackCustomValue();
        customValue.setId("LAYoUT202609072220531364");

        saveCallRecord(type, customValue);

        assertEquals("LAYoUT202609072220531364", captureRecord().getSourceId());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "generateMaskFilesAsync",
            "generateMaskFilesSync",
            "convertGrayImgToSvgAsync",
            "convertGrayImgToSvg"
    })
    void saveCallRecordUsesOrderItemIdAsPreprocessingSourceId(String type) {
        CallbackCustomValue customValue = new CallbackCustomValue();
        customValue.setId("order-item-1:request-1");
        customValue.setOrderItemId("order-item-1");

        saveCallRecord(type, customValue);

        assertEquals("order-item-1", captureRecord().getSourceId());
    }

    private void saveCallRecord(String type, CallbackCustomValue customValue) {
        CallbackConfig callbackConfig = new CallbackConfig();
        callbackConfig.setCallbackCustomValue(customValue);
        ReflectionTestUtils.invokeMethod(
                service, "saveCallRecord", "async", "url", "apiPath",
                new TestRequest(callbackConfig), type);
    }

    private AlgorithmCoreApiCallRecord captureRecord() {
        ArgumentCaptor<AlgorithmCoreApiCallRecord> captor =
                ArgumentCaptor.forClass(AlgorithmCoreApiCallRecord.class);
        verify(repository).add(captor.capture());
        return captor.getValue();
    }

    public static class TestRequest {
        private final CallbackConfig callbackConfig;

        private TestRequest(CallbackConfig callbackConfig) {
            this.callbackConfig = callbackConfig;
        }

        public CallbackConfig getCallbackConfig() {
            return callbackConfig;
        }
    }
}

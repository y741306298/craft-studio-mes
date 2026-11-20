package com.mes.application.command.delivery;

import com.mes.application.command.delivery.packaging.PackagingOperation;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryRecord;
import com.mes.domain.delivery.deliveryPkg.enums.PreOrderLabelConsumeStatus;
import com.mes.domain.delivery.deliveryPkg.repository.DeliveryRecordRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class Kuaidi100LabelReplacementTest {
    private final AppDeliveryPkgService service = new AppDeliveryPkgService();
    private final PackagingOperation packaging = mock(PackagingOperation.class);
    private final DeliveryRecordRepository records = mock(DeliveryRecordRepository.class);
    private final DeliveryRecord oldRecord = new DeliveryRecord();
    private final DeliveryRecord claimedRecord = new DeliveryRecord();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "packagingOperation", packaging);
        ReflectionTestUtils.setField(service, "deliveryRecordRepository", records);
        oldRecord.setId("record");
        oldRecord.setKuaidiNum("old-waybill");
        oldRecord.setTaskId("old-task");
        oldRecord.setConsumeStatus(PreOrderLabelConsumeStatus.PRINT_FAILED);
        claimedRecord.setTaskId("old-task");
        when(records.claimForPrinting("record", PreOrderLabelConsumeStatus.PRINT_FAILED, "printer"))
                .thenReturn(claimedRecord);
    }

    @Test
    void missingUpstreamTaskLeavesOldLabelFailedAndEnablesReplacement() {
        try (var ignored = reprintResponse(30009, "查无此单")) {
            assertNull(consumeLabel());
        }
        assertEquals(PreOrderLabelConsumeStatus.PRINT_FAILED, claimedRecord.getConsumeStatus());
        assertEquals("查无此单", claimedRecord.getErrorMsg());
        verify(records).update(claimedRecord);
        verify(packaging).beginLabelReplacement("old-waybill");
    }

    @Test
    void otherReprintErrorsDoNotEnableReplacement() {
        try (var ignored = reprintResponse(30008, "打印失败")) {
            assertThrows(BusinessNotAllowException.class, this::consumeLabel);
        }
        assertEquals(PreOrderLabelConsumeStatus.PRINT_FAILED, claimedRecord.getConsumeStatus());
        verifyNoInteractions(packaging);
    }

    private DeliveryRecord consumeLabel() {
        return ReflectionTestUtils.invokeMethod(service, "consumePreOrderLabel", oldRecord, "order", "device", "printer");
    }

    private MockedConstruction<RestTemplate> reprintResponse(int code, String message) {
        String body = """
                {"success":false,"code":%d,"message":"%s"}
                """.formatted(code, message);
        return mockConstruction(RestTemplate.class, (http, context) ->
                when(http.postForEntity(anyString(), any(), eq(String.class))).thenReturn(ResponseEntity.ok(body)));
    }
}

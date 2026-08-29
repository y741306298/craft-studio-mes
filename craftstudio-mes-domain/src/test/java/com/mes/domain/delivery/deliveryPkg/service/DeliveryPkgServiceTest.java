package com.mes.domain.delivery.deliveryPkg.service;

import com.mes.domain.delivery.deliveryPkg.repository.DeliveryPkgRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeliveryPkgServiceTest {

    @Mock
    private DeliveryPkgRepository deliveryPkgRepository;

    @InjectMocks
    private DeliveryPkgService deliveryPkgService;

    @Test
    void queryAllByConditionsAcceptsIso8601CreationTimes() {
        String start = "2026-08-28T00:00:00Z";
        String end = "2026-08-28T23:59:59Z";

        deliveryPkgService.queryAllByConditions(null, null, null, null, null, null, null, start, end);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> filtersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(deliveryPkgRepository).findAllByConditions(filtersCaptor.capture());
        assertEquals(Date.from(Instant.parse(start)), filtersCaptor.getValue().get("createTime_gte"));
        assertEquals(Date.from(Instant.parse(end)), filtersCaptor.getValue().get("createTime_lte"));
    }
}

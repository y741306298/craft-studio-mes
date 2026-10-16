package com.mes.application.command.order;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderTransferRecord.repository.OrderTransferRecordRepository;
import com.mes.domain.order.orderTransferRecord.service.OrderTransferRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AppOrderServiceCancelTransferTest {
    private AppOrderService service;
    private OrderTransferRecordRepository transferRepository;
    private OrderInfoService orderInfoService;

    @BeforeEach
    void setUp() {
        service = new AppOrderService();
        transferRepository = mock(OrderTransferRecordRepository.class);
        orderInfoService = mock(OrderInfoService.class);
        OrderTransferRecordService transferService = new OrderTransferRecordService();
        ReflectionTestUtils.setField(transferService, "orderTransferRecordRepository", transferRepository);
        ReflectionTestUtils.setField(service, "orderTransferRecordService", transferService);
        ReflectionTestUtils.setField(service, "domainOrderInfoService", orderInfoService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"orderId", "targetOrderId"})
    void rejectsTransferredOrdersEvenWhenOriginalOrderWasDeleted(String transferOrderField) {
        when(transferRepository.filterTotal(Map.of(transferOrderField, "ORDER-1"))).thenReturn(1L);

        ApiResponse<String> response = service.cancelOrder(null, "ORDER-1");

        assertEquals(ApiResponse.RepStatusCode.serviceError, response.getCode());
        assertEquals("该订单已经生产，无法退单", response.getMessage());
        verifyNoInteractions(orderInfoService);
    }

    @Test
    void continuesNormalOrderLookupWhenNoTransferRecordExists() {
        ApiResponse<String> response = service.cancelOrder("PLATFORM", "ORDER-1");

        assertEquals("订单不存在：ORDER-1", response.getMessage());
        verify(orderInfoService).findByOrderIdAndPlatformCode("ORDER-1", "PLATFORM");
    }

    @Test
    void rejectsBlankOrderIdBeforeQueryingTransfers() {
        ApiResponse<String> response = service.cancelOrder(null, " ");

        assertEquals(ApiResponse.RepStatusCode.badParams, response.getCode());
        verifyNoInteractions(transferRepository, orderInfoService);
    }
}

package com.mes.application.command.orderPreprocessing;

import com.mes.application.command.api.resp.ImageMaskResponse;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppOrderPreprocessingServiceCallbackTest {

    private AppOrderPreprocessingService service;
    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private OrderItemService orderItemService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        service = new AppOrderPreprocessingService();
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        orderItemService = mock(OrderItemService.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(service, "orderItemService", orderItemService);
    }

    @Test
    void ignoresConcurrentCallbackWhenSameRequestIsAlreadyBeingProcessed() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.MINUTES)))
                .thenReturn(false);

        service.handleGenerateMaskFilesCallback(callback("item-1", "request-1"), "item-1#request-1");

        verify(orderItemService, never()).findByOrderItemId(anyString());
        verify(redisTemplate, never()).hasKey(anyString());
    }

    @Test
    void ignoresCallbackThatWasAlreadyCompleted() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.MINUTES)))
                .thenReturn(true);
        when(redisTemplate.hasKey("orderPreprocessing:maskCallback:completed:item-1:request-1"))
                .thenReturn(true);

        service.handleGenerateMaskFilesCallback(callback("item-1", "request-1"), "item-1#request-1");

        verify(orderItemService, never()).findByOrderItemId(anyString());
        verify(valueOperations, never()).set(
                eq("orderPreprocessing:maskCallback:completed:item-1:request-1"),
                any(), anyLong(), eq(TimeUnit.DAYS));
    }

    private ImageMaskResponse callback(String orderItemId, String preprocessRequestId) {
        ImageMaskResponse response = new ImageMaskResponse();
        response.setOrderItemId(orderItemId);
        response.setPreprocessRequestId(preprocessRequestId);
        return response;
    }
}

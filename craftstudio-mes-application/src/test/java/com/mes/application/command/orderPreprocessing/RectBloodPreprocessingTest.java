package com.mes.application.command.orderPreprocessing;

import com.alibaba.fastjson2.JSON;
import com.mes.application.command.api.AlgorithmCoreApiService;
import com.mes.application.command.api.req.RectBloodRequest;
import com.mes.application.command.api.resp.RectBloodResponse;
import com.mes.application.command.api.vo.CallbackCustomValue;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import com.piliofpala.craftstudio.shared.infra.cloud.platforms.alicloud.AliCloudAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RectBloodPreprocessingTest {
    private AppOrderPreprocessingService service;
    private AlgorithmCoreApiService algorithm;
    private OrderItemService items;
    private RedisTemplate<String, Object> redis;
    private OrderItem item;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new AppOrderPreprocessingService();
        algorithm = mock(AlgorithmCoreApiService.class);
        items = mock(OrderItemService.class);
        redis = mock(RedisTemplate.class, RETURNS_DEEP_STUBS);
        ReflectionTestUtils.setField(service, "algorithmCoreApiService", algorithm);
        ReflectionTestUtils.setField(service, "orderItemService", items);
        ReflectionTestUtils.setField(service, "orderInfoService", mock(OrderInfoService.class));
        ReflectionTestUtils.setField(service, "aliCloudAuthService", mock(AliCloudAuthService.class));
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        ReflectionTestUtils.setField(service, "generateMaskFilesApiUrl",
                "https://mes.example/order/callback/generate_mask_files");
        item = new OrderItem();
        item.setId("db-1");
        item.setOrderItemId("item-1");
        item.setOrderId("order-1");
        item.setManufacturerId("factory-1");
        item.setPreprocessRequestId("request-1");
        ImageFile image = new ImageFile();
        image.setRawFile("https://images.example/input.tif");
        item.setProductionImgFile(image);
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(List.of(JSON.parseObject("""
                {"nodeName":"矩形旗缝套筒","paramConfigs":[
                  {"type":"RECT_EDGE_SELECTION","param":{"edges":["TOP"]}}
                ]}
                """, ProcedureFlowNode.class)));
        item.setProcedureFlow(flow);
        when(items.findByOrderItemId("item-1")).thenReturn(item);
        when(redis.opsForValue().setIfAbsent(anyString(), any(), eq(10L), eq(TimeUnit.MINUTES)))
                .thenReturn(true);
    }

    @Test
    void submitsAsyncAndWaitsWithVersionedCallbackId() {
        assertFalse(service.generateRectBloodIfNecessary(item));
        ArgumentCaptor<RectBloodRequest> request = ArgumentCaptor.forClass(RectBloodRequest.class);
        verify(algorithm).generateRectBloodAsync(request.capture());
        assertEquals("https://images.example/input.tif", request.getValue().getImageUrl());
        assertEquals("https://mes.example/order/callback/rect_blood",
                request.getValue().getCallbackConfig().getCallbackUrl());
        assertEquals("item-1#request-1", request.getValue().getCallbackConfig().getCallbackCustomValue().getId());
        assertEquals("orderItem/rectBlood/factory-1/item-1/request-1/", request.getValue().getUploadConfig().getUploadPath());
    }

    @Test
    void callbackUpdatesBothImagesBeforeEnqueuingAndDoesNotExpandAgain() {
        service.handleRectBloodCallback(response(), updated -> {
            assertEquals("flag.tif", updated.getProductionImgFile().getRawFile());
            assertEquals("flag.tif", updated.getProductionImgFile().getFilePreview().getRaw());
            assertEquals("mirror.tif", updated.getRectBloodMirrorDownloadUrl());
            verify(items).updateOrderItem(updated);
        });
        assertTrue(service.generateRectBloodIfNecessary(item));
        verifyNoInteractions(algorithm);
    }

    @Test
    void combinedColorWhiteColorProcessIsNotResubmittedAfterRectBlood() {
        service.handleRectBloodCallback(response(), updated -> {});
        item.getProcedureFlow().setNodes(List.of(
                item.getProcedureFlow().getNodes().get(0),
                JSON.parseObject("{\"nodeName\":\"弱溶剂彩白彩正喷\"}", ProcedureFlowNode.class)));
        item.setColorWhiteColorDownloadUrl("cwc.tif");
        assertTrue(service.convertColorWhiteColorIfNecessary(item));
        verifyNoInteractions(algorithm);
    }

    @Test
    @SuppressWarnings("unchecked")
    void staleCancelledReturnedAndRepeatedCallbacksDoNotContinue() {
        Consumer<OrderItem> continuation = mock(Consumer.class);
        RectBloodResponse response = response();
        response.getData().getCallbackCustomValue().setId("item-1#old-request");
        service.handleRectBloodCallback(response, continuation);
        service.cancelOrderItems(List.of("item-1"));
        service.handleRectBloodCallback(response(), continuation);
        service.clearCancelledOrderItems(List.of("item-1"));
        item.setStatus(OrderStatus.RETURNED);
        service.handleRectBloodCallback(response(), continuation);
        item.setStatus(null);
        when(redis.hasKey(anyString())).thenReturn(true);
        service.handleRectBloodCallback(response(), continuation);
        verify(items, never()).updateOrderItem(any());
        verifyNoInteractions(continuation);
    }

    @Test
    void failedSubmissionAndMalformedCallbackFailWithoutContinuing() {
        RectBloodResponse rejected = new RectBloodResponse();
        rejected.setCode(500);
        when(algorithm.generateRectBloodAsync(any())).thenReturn(rejected);
        assertThrows(RuntimeException.class, () -> service.generateRectBloodIfNecessary(item));
        assertThrows(IllegalArgumentException.class, () -> service.handleRectBloodCallback(rejected, updated -> fail()));
        verify(items, never()).updateOrderItem(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void failedContinuationCanRetrySavedResult() {
        Consumer<OrderItem> continuation = mock(Consumer.class);
        doThrow(new IllegalStateException("queue full")).doNothing().when(continuation).accept(item);
        assertThrows(IllegalStateException.class, () -> service.handleRectBloodCallback(response(), continuation));
        verify(redis.opsForValue(), never()).set(contains(":completed"), any(), anyLong(), any(TimeUnit.class));
        service.handleRectBloodCallback(response(), continuation);
        verify(continuation, times(2)).accept(item);
    }

    private RectBloodResponse response() {
        CallbackCustomValue custom = new CallbackCustomValue();
        custom.setId("item-1#request-1");
        RectBloodResponse.Data data = new RectBloodResponse.Data();
        data.setFlagUrl("flag.tif");
        data.setMirrorFlagUrl("mirror.tif");
        data.setCallbackCustomValue(custom);
        RectBloodResponse response = new RectBloodResponse();
        response.setCode(200);
        response.setData(data);
        return response;
    }
}

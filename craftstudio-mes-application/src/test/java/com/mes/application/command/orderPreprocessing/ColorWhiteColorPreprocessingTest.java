package com.mes.application.command.orderPreprocessing;

import com.mes.application.command.api.AlgorithmCoreApiService;
import com.mes.application.command.api.req.ColorWhiteColorRequest;
import com.mes.application.command.api.resp.ColorWhiteColorResponse;
import com.mes.application.command.api.vo.CallbackCustomValue;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
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

class ColorWhiteColorPreprocessingTest {
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
                "https://mes.example/api/manufacturerSide/order/callback/generate_mask_files");
        item = new OrderItem();
        item.setOrderItemId("item-1");
        item.setOrderId("order-1");
        item.setManufacturerId("factory-1");
        item.setPreprocessRequestId("request-1");
        ImageFile image = new ImageFile();
        image.setRawFile("https://images.example/input.tif");
        item.setProductionImgFile(image);
        ProcedureFlowNode node = new ProcedureFlowNode();
        node.setNodeName("弱溶剂彩白彩正喷");
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(List.of(node));
        item.setProcedureFlow(flow);
        when(items.findByOrderItemId("item-1")).thenReturn(item);
        when(redis.opsForValue().setIfAbsent(anyString(), any(), eq(10L), eq(TimeUnit.MINUTES)))
                .thenReturn(true);
    }

    @Test
    void submitsConversionAndWaitsBeforeContinuing() {
        assertFalse(service.convertColorWhiteColorIfNecessary(item));
        ArgumentCaptor<ColorWhiteColorRequest> request = ArgumentCaptor.forClass(ColorWhiteColorRequest.class);
        verify(algorithm).convertColorWhiteColorAsync(request.capture());
        assertEquals(item.getProductionImgFile().getRawFile(), request.getValue().getImageUrl());
        assertEquals("pieceImg/factory-1/", request.getValue().getUploadConfig().getUploadPath());
        assertEquals("item-1", request.getValue().getCallbackConfig().getCallbackCustomValue().getId());
        assertEquals("request-1", request.getValue().getCallbackConfig().getCallbackCustomValue().getPreprocessRequestId());
        assertEquals("https://mes.example/api/manufacturerSide/order/callback/convert_color_white_color",
                request.getValue().getCallbackConfig().getCallbackUrl());
    }

    @Test
    void previousProcessNameDoesNotTriggerConversion() {
        item.getProcedureFlow().getNodes().get(0).setNodeName("弱溶剂彩白彩");
        assertTrue(service.convertColorWhiteColorIfNecessary(item));
        verifyNoInteractions(algorithm);
    }

    @Test
    void ordinaryItemsContinueWithoutConversion() {
        item.setProcedureFlow(null);
        assertTrue(service.convertColorWhiteColorIfNecessary(item));
        verifyNoInteractions(algorithm);
    }

    @Test
    void callbackPersistsBothImageFieldsBeforeContinuingAndSkipsReconversion() {
        service.handleConvertColorWhiteColorCallback(response(), updated -> {
            verify(items).updateOrderItem(updated);
            assertEquals("https://images.example/cwc.tif", updated.getProductionImgFile().getRawFile());
            assertEquals("https://images.example/cwc.tif", updated.getProductionImgFile().getFilePreview().getRaw());
        });
        assertTrue(service.convertColorWhiteColorIfNecessary(item));
        verifyNoInteractions(algorithm);
    }

    @Test
    @SuppressWarnings("unchecked")
    void staleOrFailedCallbackDoesNotUpdateOrContinue() {
        Consumer<OrderItem> continuation = mock(Consumer.class);
        ColorWhiteColorResponse response = response();
        response.setCode(500);
        assertThrows(RuntimeException.class, () -> service.handleConvertColorWhiteColorCallback(response, continuation));
        response.setCode(200);
        response.getData().getCallbackCustomValue().setPreprocessRequestId("old-request");
        service.handleConvertColorWhiteColorCallback(response, continuation);
        verify(items, never()).updateOrderItem(any());
        verifyNoInteractions(continuation);
    }

    @Test
    @SuppressWarnings("unchecked")
    void repeatedCallbackDoesNotContinueAgain() {
        when(redis.hasKey(anyString())).thenReturn(true);
        Consumer<OrderItem> continuation = mock(Consumer.class);
        service.handleConvertColorWhiteColorCallback(response(), continuation);
        verify(items, never()).updateOrderItem(any());
        verifyNoInteractions(continuation);
    }

    private ColorWhiteColorResponse response() {
        CallbackCustomValue custom = new CallbackCustomValue();
        custom.setId("item-1");
        custom.setPreprocessRequestId("request-1");
        ColorWhiteColorResponse.Data data = new ColorWhiteColorResponse.Data();
        data.setDownloadUrl("https://images.example/cwc.tif");
        data.setCallbackCustomValue(custom);
        ColorWhiteColorResponse response = new ColorWhiteColorResponse();
        response.setCode(200);
        response.setData(data);
        return response;
    }
}

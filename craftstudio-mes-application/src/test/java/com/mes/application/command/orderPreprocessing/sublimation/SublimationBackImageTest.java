package com.mes.application.command.orderPreprocessing.sublimation;

import com.alibaba.fastjson2.JSON;
import com.mes.application.command.api.AlgorithmCoreApiService;
import com.mes.application.command.api.req.RectBloodRequest;
import com.mes.application.command.api.resp.RectBloodResponse;
import com.mes.application.command.api.vo.CallbackCustomValue;
import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.application.command.orderPreprocessing.strategy.SublimationOrderItemProcessingStrategy;
import com.mes.application.command.orderPreprocessing.vo.ProductionImageSize;
import com.mes.domain.manufacturer.procedure.service.ProcedureService;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.piliofpala.craftstudio.shared.infra.cloud.platforms.alicloud.AliCloudAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SublimationBackImageTest {
    private AppOrderPreprocessingService service;
    private AlgorithmCoreApiService algorithm;
    private OrderItemService items;
    private final Set<String> completedKeys = new HashSet<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        service = new AppOrderPreprocessingService();
        algorithm = mock(AlgorithmCoreApiService.class);
        items = mock(OrderItemService.class);
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(redis.hasKey(anyString())).thenAnswer(call -> completedKeys.contains(call.getArgument(0)));
        doAnswer(call -> { completedKeys.add(call.getArgument(0)); return null; })
                .when(values).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        ReflectionTestUtils.setField(service, "algorithmCoreApiService", algorithm);
        ReflectionTestUtils.setField(service, "orderItemService", items);
        ReflectionTestUtils.setField(service, "orderInfoService", mock(OrderInfoService.class));
        ReflectionTestUtils.setField(service, "aliCloudAuthService", mock(AliCloudAuthService.class));
        ReflectionTestUtils.setField(service, "rectBloodCallbackUrl", "https://example.test/callback/rect_blood");
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void independentlyBleedsBothSidesAndWaitsForBothCallbacks(boolean sameImage, boolean frontFirst) {
        OrderItem item = item(sameImage);
        when(items.findByOrderItemId("item")).thenReturn(item);
        assertFalse(service.generateRectBloodIfNecessary(item));
        ArgumentCaptor<RectBloodRequest> requests = ArgumentCaptor.forClass(RectBloodRequest.class);
        verify(algorithm, times(2)).generateRectBloodAsync(requests.capture());
        RectBloodRequest front = requests.getAllValues().get(0);
        RectBloodRequest back = requests.getAllValues().get(1);
        assertEquals("front-input.tif", front.getImageUrl());
        assertEquals(sameImage ? "front-input.tif" : "back-input.tif", back.getImageUrl());
        assertEquals(front.getBloodConfig(), back.getBloodConfig());
        assertFalse(back.getBloodConfig().getMirrorRequired());
        assertEquals(back.getImageUrl(), item.getSublimationBackSourceUrl());

        @SuppressWarnings("unchecked")
        Consumer<OrderItem> continuation = mock(Consumer.class);
        RectBloodResponse frontResponse = callback(front, "front-expanded.tif");
        // 即使正面算法带回镜像图，也不能将它作为反面资源。
        frontResponse.getData().setMirrorFlagUrl("unused-mirrored.tif");
        RectBloodResponse backResponse = callback(back, "back-expanded.tif");
        service.handleRectBloodCallback(frontFirst ? frontResponse : backResponse, continuation);
        verifyNoInteractions(continuation);
        assertFalse(service.isSublimationReadyForStrategy(item, SublimationProcessStrategy.resolve(item)));
        service.handleRectBloodCallback(frontFirst ? backResponse : frontResponse, continuation);
        verify(continuation).accept(item);
        assertTrue(service.generateRectBloodIfNecessary(item));
        assertEquals("back-expanded.tif", item.getSublimationBackDownloadUrl());
        assertEquals("front-expanded.tif", item.getProductionImgFile().getRawFile());
        assertEquals("preview-original.tif", item.getProductionImgFile().getFilePreview().getRaw());
        service.handleRectBloodCallback(frontResponse, continuation);
        service.handleRectBloodCallback(backResponse, continuation);
        verify(continuation, times(1)).accept(item);
        verify(algorithm, times(2)).generateRectBloodAsync(any());
    }

    @Test
    void sameBackRetryAfterFrontCallbackUsesOriginalInput() {
        OrderItem item = item(true);
        when(items.findByOrderItemId("item")).thenReturn(item);
        when(algorithm.generateRectBloodAsync(any())).thenReturn(null)
                .thenThrow(new IllegalStateException("temporary back failure")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> service.generateRectBloodIfNecessary(item));
        ArgumentCaptor<RectBloodRequest> requests = ArgumentCaptor.forClass(RectBloodRequest.class);
        verify(algorithm, times(2)).generateRectBloodAsync(requests.capture());
        // 同画面正面无需 mirrorFlagUrl，也必须继续等待反面。
        service.handleRectBloodCallback(callback(requests.getAllValues().get(0), "front-expanded.tif"), ignored -> fail());
        assertFalse(service.generateRectBloodIfNecessary(item));
        verify(algorithm, times(3)).generateRectBloodAsync(requests.capture());
        RectBloodRequest retry = requests.getValue();
        assertEquals("front-input.tif", retry.getImageUrl());
        assertEquals(SublimationProcessStrategy.BACK_CALLBACK,
                retry.getCallbackConfig().getCallbackCustomValue().getPresetType());
    }

    @Test
    void pieceUsesOnlyIndependentBackResultInOneMirrorConfig() {
        OrderItem item = item(true);
        item.setSublimationBackSourceUrl("front-input.tif");
        item.setSublimationBackDownloadUrl("back-expanded.tif");
        item.setMirrorFlagUrl("unused-mirrored.tif");
        item.setRectBloodDownloadUrl("front-expanded.tif");
        item.getProductionImgFile().setRawFile("front-expanded.tif");
        AppOrderPreprocessingService preprocessing = mock(AppOrderPreprocessingService.class);
        when(preprocessing.isSublimationReadyForStrategy(eq(item), any())).thenReturn(true);
        when(preprocessing.resolveProductionImageSizeForStrategy(item)).thenReturn(new ProductionImageSize(1044D, 2054D));
        when(preprocessing.generateRectMaskSvgForStrategy(item)).thenReturn("front-mask.svg");
        when(preprocessing.resolveProductionImageUrlForStrategy(item)).thenReturn("front-expanded.tif");
        ProcedureService procedures = mock(ProcedureService.class);
        when(preprocessing.getProcedureService()).thenReturn(procedures);
        ProductionPiece piece = new ProductionPiece();
        when(procedures.createProductionPiece(eq(item), eq("ORIGINAL"), eq("front-expanded.tif"),
                eq(item.getProcedureFlow()), eq("front-mask.svg"), eq(1044D), eq(2054D))).thenReturn(piece);
        SublimationDecorationService decorations = mock(SublimationDecorationService.class);
        when(decorations.decorate(eq(item), eq(item.getProcedureFlow()), eq(piece), any(), eq("back-expanded.tif"), isNull()))
                .thenReturn("back-mask.svg");
        var strategy = new SublimationOrderItemProcessingStrategy(decorations, mock(OrderInfoService.class), mock(SublimationTraceService.class));
        assertEquals(List.of(piece), strategy.process(item, item.getProcedureFlow(), preprocessing));
        assertEquals(1, piece.getMirrorConfigs().size());
        assertEquals("back-expanded.tif", piece.getMirrorConfigs().getFirst().getImg());
        assertEquals("back-mask.svg", piece.getMirrorConfigs().getFirst().getSvg());
    }

    private static RectBloodResponse callback(RectBloodRequest request, String url) {
        CallbackCustomValue custom = new CallbackCustomValue();
        // 模拟算法仅透传 id，仍应正确识别面和请求版本。
        custom.setId(request.getCallbackConfig().getCallbackCustomValue().getId());
        RectBloodResponse.Data data = new RectBloodResponse.Data();
        data.setFlagUrl(url);
        data.setCallbackCustomValue(custom);
        RectBloodResponse response = new RectBloodResponse();
        response.setCode(200);
        response.setData(data);
        return response;
    }

    private static OrderItem item(boolean sameImage) {
        OrderItem item = JSON.parseObject("""
                {"id":"db-item","orderItemId":"item","manufacturerId":"factory","preprocessRequestId":"request",
                 "productionImgFile":{"rawFile":"front-input.tif","filePreview":{"raw":"preview-original.tif"}},
                 "procedureFlow":{"nodes":[
                   {"nodeName":"热升华","nodeOrder":1},
                   {"nodeName":"缝布带管套","nodeOrder":2,"paramConfigs":[
                     {"param":{"type":"RECT_EDGE_SELECTION","edges":["TOP"]}}]},
                   {"nodeName":"单针锁边","nodeOrder":3},
                   {"nodeName":"反面不同画面","nodeOrder":4,"paramConfigs":[
                     {"param":{"file":{"rawFile":"back-input.tif"}}}]}
                 ]}}
                """, OrderItem.class);
        if (sameImage) item.getProcedureFlow().getNodes().getLast().setNodeName("反面相同画面");
        return item;
    }
}

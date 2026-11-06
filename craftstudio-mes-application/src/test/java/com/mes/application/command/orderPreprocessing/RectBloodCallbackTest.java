package com.mes.application.command.orderPreprocessing;

import com.mes.application.command.api.resp.RectBloodResponse;
import com.mes.application.command.api.vo.CallbackCustomValue;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import com.piliofpala.craftstudio.shared.domain.file.vo.FilePreview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RectBloodCallbackTest {
    private AppOrderPreprocessingService service;
    private OrderItemService items;
    private OrderItem item;
    private RectBloodResponse response;

    @BeforeEach
    void setUp() {
        service = new AppOrderPreprocessingService();
        items = mock(OrderItemService.class);
        ReflectionTestUtils.setField(service, "orderItemService", items);
        item = new OrderItem();
        item.setOrderItemId("item-1");
        when(items.findByOrderItemId("item-1")).thenReturn(item);
        response = new RectBloodResponse();
        response.setCode(200);
        RectBloodResponse.Data data = new RectBloodResponse.Data();
        data.setFlagUrl("https://images.example/flag.tif");
        data.setMirrorFlagUrl("https://images.example/mirror-flag.tif");
        CallbackCustomValue customValue = new CallbackCustomValue();
        customValue.setId("item-1");
        data.setCallbackCustomValue(customValue);
        response.setData(data);
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-image", "no-preview", "existing-preview"})
    void savesBothOriginalUrlsAndMirrorBeforeReturning(String imageState) {
        if (!"no-image".equals(imageState)) {
            ImageFile image = new ImageFile();
            image.setRawFile("old-original");
            if ("existing-preview".equals(imageState)) {
                FilePreview preview = new FilePreview();
                preview.setRaw("old-preview-original");
                preview.setThumbnail("existing-thumbnail");
                image.setFilePreview(preview);
            }
            item.setProductionImgFile(image);
        }
        assertSame(item, service.handleRectBloodCallback(response));
        assertEquals(response.getData().getFlagUrl(), item.getProductionImgFile().getRawFile());
        assertEquals(response.getData().getFlagUrl(), item.getProductionImgFile().getFilePreview().getRaw());
        assertEquals(response.getData().getMirrorFlagUrl(), item.getMirrorFlagUrl());
        if ("existing-preview".equals(imageState)) {
            assertEquals("existing-thumbnail", item.getProductionImgFile().getFilePreview().getThumbnail());
        }
        verify(items).updateOrderItem(item);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "failed", "no-data", "no-custom-value", "no-id", "no-flag"})
    void rejectsInvalidCallbackWithoutUpdatingOrder(String invalid) {
        switch (invalid) {
            case "null": response = null; break;
            case "failed": response.setCode(500); break;
            case "no-data": response.setData(null); break;
            case "no-custom-value": response.getData().setCallbackCustomValue(null); break;
            case "no-id": response.getData().getCallbackCustomValue().setId(" "); break;
            case "no-flag": response.getData().setFlagUrl(" "); break;
        }
        assertThrows(RuntimeException.class, () -> service.handleRectBloodCallback(response));
        verifyNoInteractions(items);
    }

    @Test
    void rejectsUnknownOrder() {
        when(items.findByOrderItemId("item-1")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.handleRectBloodCallback(response));
        verify(items, never()).updateOrderItem(any());
    }

    @Test
    void clearsPreviousMirrorWhenCallbackHasNoMirror() {
        item.setMirrorFlagUrl("old-mirror");
        response.getData().setMirrorFlagUrl(null);
        service.handleRectBloodCallback(response);
        assertNull(item.getMirrorFlagUrl());
        verify(items).updateOrderItem(item);
    }
}

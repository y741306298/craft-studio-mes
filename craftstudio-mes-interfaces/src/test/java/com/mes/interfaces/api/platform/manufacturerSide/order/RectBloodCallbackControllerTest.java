package com.mes.interfaces.api.platform.manufacturerSide.order;

import com.mes.application.command.api.resp.RectBloodResponse;
import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.application.command.orderPreprocessing.OrderPreprocessTaskQueue;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RectBloodCallbackControllerTest {
    private OrderController controller;
    private AppOrderPreprocessingService service;
    private OrderPreprocessTaskQueue queue;

    @BeforeEach
    void setUp() {
        controller = new OrderController();
        service = mock(AppOrderPreprocessingService.class);
        queue = mock(OrderPreprocessTaskQueue.class);
        ReflectionTestUtils.setField(controller, "appOrderPreprocessingService", service);
        ReflectionTestUtils.setField(controller, "orderPreprocessTaskQueue", queue);
    }

    @Test
    void submitsUpdatedItemAfterCallbackServiceReturns() {
        RectBloodResponse response = new RectBloodResponse();
        OrderItem updated = new OrderItem();
        when(service.handleRectBloodCallback(response)).thenReturn(updated);

        assertEquals(200, controller.handleRectBloodCallback(response).getCode());

        InOrder order = inOrder(service, queue);
        order.verify(service).handleRectBloodCallback(response);
        order.verify(queue).submit(List.of(updated));
    }

    @Test
    void doesNotContinueWhenSavingFails() {
        RectBloodResponse response = new RectBloodResponse();
        when(service.handleRectBloodCallback(response)).thenThrow(new RuntimeException("保存失败"));

        assertEquals(500, controller.handleRectBloodCallback(response).getCode());
        verifyNoInteractions(queue);
    }

    @Test
    void reportsQueueSubmissionFailure() {
        RectBloodResponse response = new RectBloodResponse();
        OrderItem updated = new OrderItem();
        when(service.handleRectBloodCallback(response)).thenReturn(updated);
        doThrow(new RuntimeException("队列提交失败")).when(queue).submit(List.of(updated));

        assertEquals(500, controller.handleRectBloodCallback(response).getCode());
    }
}

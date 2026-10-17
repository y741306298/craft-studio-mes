package com.mes.application.command.orderPreprocessing;

import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ColorWhiteColorQueueTest {
    @Test
    void waitingItemIsExcludedFromGrayConversionAndMaskProcessing() throws Exception {
        AppOrderPreprocessingService service = mock(AppOrderPreprocessingService.class);
        OrderItemService items = mock(OrderItemService.class);
        OrderPreprocessTaskQueue queue = new OrderPreprocessTaskQueue(service);
        ReflectionTestUtils.setField(queue, "orderItemService", items);
        OrderItem waiting = item("waiting");
        OrderItem ordinary = item("ordinary");
        when(items.findByOrderItemId("waiting")).thenReturn(waiting);
        when(items.findByOrderItemId("ordinary")).thenReturn(ordinary);
        when(service.convertColorWhiteColorIfNecessary(waiting)).thenReturn(false);
        when(service.convertColorWhiteColorIfNecessary(ordinary)).thenReturn(true);
        when(service.convertMaskGrayImgToSvgIfNecessary(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Class<?> taskType = Class.forName(OrderPreprocessTaskQueue.class.getName() + "$OrderPreprocessTask");
        Constructor<?> constructor = taskType.getDeclaredConstructor(List.class, int.class);
        constructor.setAccessible(true);
        Object task = constructor.newInstance(List.of(waiting, ordinary), 0);

        assertEquals(Boolean.TRUE, ReflectionTestUtils.invokeMethod(queue, "handleTask", task));

        verify(service).convertMaskGrayImgToSvgIfNecessary(List.of(ordinary));
        verify(service).preprocessOrder(List.of(ordinary));
        queue.stop();
    }

    @Test
    @SuppressWarnings("unchecked")
    void earlyCallbackContinuationIsEnqueuedEvenIfOriginalItemIsStillQueued() {
        OrderPreprocessTaskQueue queue = new OrderPreprocessTaskQueue(mock(AppOrderPreprocessingService.class));
        LinkedBlockingQueue<Object> pending = new LinkedBlockingQueue<>(2);
        ReflectionTestUtils.setField(queue, "queue", pending);
        java.util.Set<String> queuedIds = (java.util.Set<String>) ReflectionTestUtils.getField(queue, "queuedOrderItemIds");
        queuedIds.add("waiting");

        queue.submitConvertedOrderItem(item("waiting"));

        assertEquals(1, pending.size());
        queue.stop();
    }

    private OrderItem item(String id) {
        OrderItem item = new OrderItem();
        item.setOrderItemId(id);
        return item;
    }
}

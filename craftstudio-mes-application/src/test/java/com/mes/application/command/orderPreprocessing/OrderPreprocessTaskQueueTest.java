package com.mes.application.command.orderPreprocessing;

import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderPreprocessTaskQueueTest {

    @Test
    void shouldOnlyMarkOrderItemFailedAfterRetriesAreExhausted() throws Exception {
        AppOrderPreprocessingService preprocessingService = mock(AppOrderPreprocessingService.class);
        OrderItemService orderItemService = mock(OrderItemService.class);
        OrderPreprocessTaskQueue taskQueue = new OrderPreprocessTaskQueue(preprocessingService);
        ReflectionTestUtils.setField(taskQueue, "orderItemService", orderItemService);
        ReflectionTestUtils.setField(taskQueue, "queue", new LinkedBlockingQueue<>());
        ReflectionTestUtils.setField(taskQueue, "maxRetry", 3);
        ReflectionTestUtils.setField(taskQueue, "retryBackoffMs", 0L);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId("item-1");
        List<OrderItem> batch = List.of(orderItem);
        when(preprocessingService.convertMaskGrayImgToSvgIfNecessary(batch)).thenReturn(batch);
        doThrow(new RuntimeException("temporary failure"))
                .when(preprocessingService).preprocessOrder(batch);

        assertThat(handleTask(taskQueue, task(batch, 0))).isFalse();
        verify(orderItemService, never()).markAsFailed("item-1", "temporary failure");

        assertThat(handleTask(taskQueue, task(batch, 3))).isTrue();
        verify(orderItemService).markAsFailed("item-1", "temporary failure");
    }

    private boolean handleTask(OrderPreprocessTaskQueue queue, Object task) {
        return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(queue, "handleTask", task));
    }

    private Object task(List<OrderItem> orderItems, int retryCount) throws Exception {
        Class<?> taskClass = Class.forName(OrderPreprocessTaskQueue.class.getName() + "$OrderPreprocessTask");
        Constructor<?> constructor = taskClass.getDeclaredConstructor(List.class, int.class);
        constructor.setAccessible(true);
        return constructor.newInstance(orderItems, retryCount);
    }
}

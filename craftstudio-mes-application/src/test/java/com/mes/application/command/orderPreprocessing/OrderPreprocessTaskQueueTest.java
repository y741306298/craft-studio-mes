package com.mes.application.command.orderPreprocessing;

import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderPreprocessTaskQueueTest {

    @Test
    void recoveryLoadsConfiguredBatchWithoutPaginatedQueryLimit() {
        AppOrderPreprocessingService preprocessingService = mock(AppOrderPreprocessingService.class);
        OrderItemService orderItemService = mock(OrderItemService.class);
        OrderPreprocessTaskQueue taskQueue = new OrderPreprocessTaskQueue(preprocessingService);
        BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
        ReflectionTestUtils.setField(taskQueue, "orderItemService", orderItemService);
        ReflectionTestUtils.setField(taskQueue, "queue", queue);
        ReflectionTestUtils.setField(taskQueue, "batchSize", 1_000);
        ReflectionTestUtils.setField(taskQueue, "pendingRecoveryAgeMs", 1_000L);

        when(orderItemService.findStalePendingItems(any(Date.class), 1_000))
                .thenReturn(orderItems(0, 1_000));

        try {
            taskQueue.recoverStalePendingItems();

            assertThat(queue).hasSize(1);
            verify(orderItemService).findStalePendingItems(any(Date.class), 1_000);
        } finally {
            taskQueue.stop();
        }
    }

    private List<OrderItem> orderItems(int start, int count) {
        return IntStream.range(start, start + count)
                .mapToObj(index -> {
                    OrderItem item = new OrderItem();
                    item.setOrderItemId("item-" + index);
                    return item;
                })
                .toList();
    }
}

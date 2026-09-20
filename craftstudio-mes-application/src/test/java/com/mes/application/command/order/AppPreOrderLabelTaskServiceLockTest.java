package com.mes.application.command.order;

import com.mes.domain.order.preOrderLabelTask.entity.PreOrderLabelTask;
import com.mes.domain.order.preOrderLabelTask.service.PreOrderLabelTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppPreOrderLabelTaskServiceLockTest {

    @Test
    void skipsTaskAlreadyClaimedByAnotherScheduler() {
        AppPreOrderLabelTaskService service = new AppPreOrderLabelTaskService();
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        PreOrderLabelTaskService taskService = mock(PreOrderLabelTaskService.class);
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(service, "preOrderLabelTaskService", taskService);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), eq(2L), eq(TimeUnit.HOURS)))
                .thenReturn(false);
        PreOrderLabelTask task = new PreOrderLabelTask();
        task.setId("task-1");
        task.setOrderId("order-1");

        ReflectionTestUtils.invokeMethod(service, "processTaskWithLock", task);

        verify(valueOperations).setIfAbsent(
                eq("preOrderLabelTask:processing:task-1"), anyString(), eq(2L), eq(TimeUnit.HOURS));
        verify(taskService, never()).markFailed(task, null, "订单ID为空");
    }
}

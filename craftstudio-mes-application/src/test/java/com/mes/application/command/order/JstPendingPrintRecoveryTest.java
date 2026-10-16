package com.mes.application.command.order;

import com.mes.application.command.delivery.AppDeliveryPkgService;
import com.mes.application.command.delivery.JstPrintGuard;
import com.mes.application.command.delivery.JstPrintUnavailableException;
import com.mes.domain.gatherplatform.wdt.entity.WdtConfig;
import com.mes.domain.gatherplatform.wdt.entity.WdtLabelRecord;
import com.mes.domain.gatherplatform.wdt.repository.WdtLabelRecordRepository;
import com.mes.domain.gatherplatform.wdt.service.WdtService;
import com.mes.domain.order.enums.OrderChannelType;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.vo.OrderChannelInfo;
import com.mes.domain.order.orderInfo.vo.LogisticsCarrierInfo;
import com.mes.domain.order.preOrderLabelTask.entity.PreOrderLabelTask;
import com.mes.domain.order.preOrderLabelTask.enums.PreOrderLabelTaskStatus;
import com.mes.domain.order.preOrderLabelTask.service.PreOrderLabelTaskService;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.auth.SmsCodeRequiredAuthException;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JstPendingPrintRecoveryTest {
    private AppPreOrderLabelTaskService service;
    private PreOrderLabelTaskService tasks;
    private OrderInfoService orders;
    private AppDeliveryPkgService delivery;
    private WdtLabelRecordRepository labels;
    private RedisTemplate<String, Object> redis;
    private JstPrintGuard guard;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        service = new AppPreOrderLabelTaskService();
        tasks = mock(PreOrderLabelTaskService.class);
        orders = mock(OrderInfoService.class);
        delivery = mock(AppDeliveryPkgService.class);
        labels = mock(WdtLabelRecordRepository.class);
        guard = mock(JstPrintGuard.class);
        redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        WdtService configs = mock(WdtService.class);
        when(configs.findConfig(eq("JST"), anyString(), anyString())).thenReturn(new WdtConfig());
        ReflectionTestUtils.setField(service, "preOrderLabelTaskService", tasks);
        ReflectionTestUtils.setField(service, "orderInfoService", orders);
        ReflectionTestUtils.setField(service, "appDeliveryPkgService", delivery);
        ReflectionTestUtils.setField(service, "wdtLabelRecordRepository", labels);
        ReflectionTestUtils.setField(service, "wdtService", configs);
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        ReflectionTestUtils.setField(service, "jstPrintGuard", guard);
    }

    @Test
    void automaticallySkipsWdtAndAlreadyPrintedJstAndAttemptsOnlyOneOrder() {
        PreOrderLabelTask wdt = task("wdt", "WDT");
        PreOrderLabelTask printed = task("printed", "JST");
        PreOrderLabelTask selected = task("selected", "JST");
        PreOrderLabelTask later = task("later", "JST");
        when(tasks.findPendingTasks(1, 100)).thenReturn(List.of(wdt, printed, selected, later));
        when(labels.findForPlatform("JST", "printed", "channel-printed", null)).thenReturn(new WdtLabelRecord());
        WdtLabelRecord result = new WdtLabelRecord();
        when(delivery.retryJstPrintForSmsAuth("selected")).thenReturn(result);
        assertSame(result, service.retryJstPrintForSmsAuth());
        verify(delivery).retryJstPrintForSmsAuth("selected");
        verifyNoMoreInteractions(delivery);
        verify(tasks, never()).markProcessed(any(), any());
        verify(tasks, never()).markFailed(any(), any(), any());
    }

    @Test
    void scansNextPageRatherThanStoppingAtFirstHundredWdtTasks() {
        PreOrderLabelTask wdt = task("wdt", "WDT");
        PreOrderLabelTask jst = task("selected", "JST");
        when(tasks.findPendingTasks(1, 100)).thenReturn(Collections.nCopies(100, wdt));
        when(tasks.findPendingTasks(2, 100)).thenReturn(List.of(jst));
        service.retryJstPrintForSmsAuth();
        verify(delivery).retryJstPrintForSmsAuth("selected");
    }

    @Test
    void noCandidateDoesNotCallPrinting() {
        when(tasks.findPendingTasks(1, 100)).thenReturn(List.of());
        assertThrows(IllegalArgumentException.class, service::retryJstPrintForSmsAuth);
        verifyNoInteractions(delivery);
    }

    @Test
    void smsExceptionDoesNotAttemptAnotherOrderOrFailTask() {
        PreOrderLabelTask first = task("first", "JST");
        PreOrderLabelTask second = task("second", "JST");
        when(tasks.findPendingTasks(1, 100)).thenReturn(List.of(first, second));
        when(delivery.retryJstPrintForSmsAuth("first")).thenThrow(new SmsCodeRequiredAuthException("需要验证码校验身份"));
        assertThrows(SmsCodeRequiredAuthException.class, service::retryJstPrintForSmsAuth);
        verify(delivery, never()).retryJstPrintForSmsAuth("second");
        assertEquals(PreOrderLabelTaskStatus.PENDING, first.getStatus());
        verify(tasks, never()).markFailed(any(), any(), any());
    }

    @Test
    void repeatedlyLockedSchedulerTaskRemainsPendingWithoutRetryCounter() {
        PreOrderLabelTask task = task("locked", "JST");
        doThrow(new JstPrintUnavailableException("请联系客服校验身份"))
                .when(guard).assertPrintingAllowed(GatherPlatformType.JST);
        for (int attempt = 0; attempt < 5; attempt++) {
            ReflectionTestUtils.invokeMethod(service, "processTask", task);
        }
        assertEquals(PreOrderLabelTaskStatus.PENDING, task.getStatus());
        verifyNoInteractions(redis, delivery, labels);
        verify(tasks, never()).markFailed(any(), any(), any());
    }

    @Test
    void ordinaryJstPrintFailuresFailOnlyOnThirdAttempt() {
        PreOrderLabelTask task = task("jst", "JST");
        OrderInfo order = orders.findByOrderId("jst");
        when(redis.opsForValue().increment("preOrderLabelTask:wdtPrintRetry:jst")).thenReturn(1L, 2L, 3L);
        for (int attempt = 0; attempt < 2; attempt++) {
            ReflectionTestUtils.invokeMethod(service, "handleGatherPrintFailure", task, order, "打印失败");
        }
        verify(tasks, never()).markFailed(any(), any(), any());
        ReflectionTestUtils.invokeMethod(service, "handleGatherPrintFailure", task, order, "打印失败");
        verify(redis.opsForValue(), times(3)).increment("preOrderLabelTask:wdtPrintRetry:jst");
        verify(tasks).markFailed(task, null, "打印失败");
        verify(orders).updateOrder(order);
    }

    @Test
    void wdtStillUsesOriginalRetryCounter() {
        PreOrderLabelTask task = task("wdt", "WDT");
        OrderInfo order = orders.findByOrderId("wdt");
        when(redis.opsForValue().increment("preOrderLabelTask:wdtPrintRetry:wdt")).thenReturn(1L);
        ReflectionTestUtils.invokeMethod(service, "handleGatherPrintFailure", task, order, "打印失败");
        verify(redis.opsForValue()).increment("preOrderLabelTask:wdtPrintRetry:wdt");
        verify(tasks, never()).markFailed(any(), any(), any());
    }

    private PreOrderLabelTask task(String id, String code) {
        PreOrderLabelTask task = new PreOrderLabelTask();
        task.setId(id);
        task.setOrderId(id);
        task.setStatus(PreOrderLabelTaskStatus.PENDING);
        OrderChannelInfo channel = new OrderChannelInfo();
        channel.setType(OrderChannelType.GATHER_PLATFORM);
        channel.setCode(code);
        channel.setOrderId("channel-" + id);
        OrderInfo order = new OrderInfo();
        order.setOrderId(id);
        order.setChannel(channel);
        order.setManufacturerId("factory");
        LogisticsCarrierInfo carrier = new LogisticsCarrierInfo();
        carrier.setPresetType("ZTO");
        order.setLogisticsCarrierInfo(carrier);
        when(orders.findByOrderId(id)).thenReturn(order);
        when(tasks.findById(id)).thenReturn(task);
        return task;
    }
}

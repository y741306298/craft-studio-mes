package com.mes.application.command.delivery;

import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.auth.AuthException;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.auth.SmsCodeRequiredAuthException;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator.WebSimulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 所有上游调用都使用 mock，不发送短信、不生成真实面单。 */
class JstPrintGuardTest {
    private RedisTemplate<String, Object> redis;
    private ValueOperations<String, Object> values;
    private JstPrintGuard guard;
    private final AtomicBoolean locked = new AtomicBoolean();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        redis = mock(RedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(eq(JstPrintGuard.OPERATION_KEY), anyString(), anyLong(), any())).thenReturn(true);
        when(redis.hasKey(JstPrintGuard.AUTH_LOCK_KEY)).thenAnswer(invocation -> locked.get());
        doAnswer(invocation -> { locked.set(true); return null; })
                .when(values).set(JstPrintGuard.AUTH_LOCK_KEY, "required");
        guard = new JstPrintGuard(redis);
    }

    @Test
    void smsExceptionLocksEveryGuardInstanceButWdtStillPrints() throws Exception {
        SmsCodeRequiredAuthException failure = new SmsCodeRequiredAuthException("需要验证码校验身份");
        assertSame(failure, assertThrows(SmsCodeRequiredAuthException.class,
                () -> guard.execute(GatherPlatformType.JST, () -> { throw failure; })));
        JstPrintGuard anotherInstance = new JstPrintGuard(redis);
        AtomicBoolean printed = new AtomicBoolean();
        assertThrows(JstPrintUnavailableException.class,
                () -> anotherInstance.execute(GatherPlatformType.JST, () -> printed.getAndSet(true)));
        assertFalse(printed.get());
        assertEquals("wdt", anotherInstance.execute(GatherPlatformType.WDT, () -> "wdt"));
        verify(values).set(JstPrintGuard.AUTH_LOCK_KEY, "required");
    }

    @Test
    void wrappedSmsExceptionAlsoLocksAndRemainsRecognizable() {
        assertThrows(SmsCodeRequiredAuthException.class, () -> guard.execute(GatherPlatformType.JST, () -> {
            throw new IllegalStateException("包装异常", new SmsCodeRequiredAuthException("需要验证码校验身份"));
        }));
        assertTrue(locked.get());
    }

    @Test
    void ordinaryPrintFailureDoesNotLock() {
        assertThrows(IllegalStateException.class,
                () -> guard.execute(GatherPlatformType.JST, () -> { throw new IllegalStateException("网络错误"); }));
        assertFalse(locked.get());
    }

    @Test
    void customerRetryBypassesLockAndSuccessDoesNotUnlock() throws Exception {
        locked.set(true);
        assertEquals("printed", guard.retryForSmsAuth(() -> "printed"));
        assertTrue(locked.get());
        verify(redis, never()).delete(JstPrintGuard.AUTH_LOCK_KEY);
        assertThrows(SmsCodeRequiredAuthException.class,
                () -> guard.retryForSmsAuth(() -> { throw new SmsCodeRequiredAuthException("需要验证码校验身份"); }));
        assertTrue(locked.get());
    }

    @Test
    void successfulSmsAuthenticationUnlocksOnlyAfterUpstreamSuccess() throws Exception {
        locked.set(true);
        WebSimulator simulator = mock(WebSimulator.class);
        doAnswer(invocation -> { verify(redis, never()).delete(JstPrintGuard.AUTH_LOCK_KEY); return null; })
                .when(simulator).obtainAuthSid("8203");
        try (MockedStatic<WebSimulator> factory = mockStatic(WebSimulator.class)) {
            factory.when(WebSimulator::getLoggedInstance).thenReturn(simulator);
            guard.authenticate("8203");
            verify(simulator).obtainAuthSid("8203");
            verify(redis).delete(JstPrintGuard.AUTH_LOCK_KEY);
        }
    }

    @Test
    void failedSmsAuthenticationKeepsLock() throws Exception {
        locked.set(true);
        WebSimulator simulator = mock(WebSimulator.class);
        doThrow(new AuthException("验证码错误")).when(simulator).obtainAuthSid("0000");
        try (MockedStatic<WebSimulator> factory = mockStatic(WebSimulator.class)) {
            factory.when(WebSimulator::getLoggedInstance).thenReturn(simulator);
            assertThrows(AuthException.class, () -> guard.authenticate("0000"));
            verify(redis, never()).delete(JstPrintGuard.AUTH_LOCK_KEY);
        }
    }

    @Test
    void concurrentOperationCannotReachUpstream() {
        when(values.setIfAbsent(eq(JstPrintGuard.OPERATION_KEY), anyString(), anyLong(), any())).thenReturn(false);
        assertThrows(JstPrintUnavailableException.class,
                () -> guard.execute(GatherPlatformType.JST, () -> fail("不得调用上游")));
        assertThrows(JstPrintUnavailableException.class,
                () -> guard.retryForSmsAuth(() -> fail("客服操作也不能与授权操作并发")));
        try (MockedStatic<WebSimulator> factory = mockStatic(WebSimulator.class)) {
            assertThrows(JstPrintUnavailableException.class, () -> guard.authenticate("8203"));
            factory.verifyNoInteractions();
        }
    }
}

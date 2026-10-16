package com.mes.application.command.delivery;

import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.auth.AuthException;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.auth.SmsCodeRequiredAuthException;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator.WebSimulator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** 所有实例共享 JST 身份锁；普通打印、客服重试与验证码校验串行执行。 */
@Service
public class JstPrintGuard {
    static final String AUTH_LOCK_KEY = "gatherPlatform:JST:smsAuthRequired";
    static final String OPERATION_KEY = "gatherPlatform:JST:printOperation";
    public static final String AUTH_REQUIRED_MESSAGE = "聚水潭打印已锁定，请联系客服校验身份";
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);
    private final RedisTemplate<String, Object> redisTemplate;

    public JstPrintGuard(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @FunctionalInterface
    public interface PrintOperation<T> {
        T run() throws Exception;
    }

    public void assertPrintingAllowed(GatherPlatformType platform) {
        if (platform == GatherPlatformType.JST && Boolean.TRUE.equals(redisTemplate.hasKey(AUTH_LOCK_KEY))) {
            throw new JstPrintUnavailableException(AUTH_REQUIRED_MESSAGE);
        }
    }

    public <T> T execute(GatherPlatformType platform, PrintOperation<T> operation) throws Exception {
        if (platform != GatherPlatformType.JST) {
            return operation.run();
        }
        return executeJst(false, operation);
    }

    /** 仅供配置端客服入口使用：绕过身份锁，仍与其他 JST 操作互斥。成功也不解锁。 */
    public <T> T retryForSmsAuth(PrintOperation<T> operation) throws Exception {
        return executeJst(true, operation);
    }

    private <T> T executeJst(boolean recovery, PrintOperation<T> operation) throws Exception {
        String token = acquireOperation();
        try {
            if (!recovery) {
                assertPrintingAllowed(GatherPlatformType.JST);
            }
            try {
                return operation.run();
            } catch (Exception exception) {
                if (requiresSmsAuth(exception)) {
                    // 不设过期时间；必须通过真实验证码校验才能解除。
                    redisTemplate.opsForValue().set(AUTH_LOCK_KEY, "required");
                    if (!(exception instanceof SmsCodeRequiredAuthException)) {
                        throw new SmsCodeRequiredAuthException("需要验证码校验身份", exception);
                    }
                }
                throw exception;
            }
        } finally {
            releaseOperation(token);
        }
    }

    /** 校验失败时保留身份锁；仅在上游校验成功后删除。 */
    public void authenticate(String smsCode) throws AuthException {
        String token = acquireOperation();
        try {
            WebSimulator.getLoggedInstance().obtainAuthSid(smsCode);
            redisTemplate.delete(AUTH_LOCK_KEY);
        } finally {
            releaseOperation(token);
        }
    }

    public static boolean requiresSmsAuth(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SmsCodeRequiredAuthException) {
                return true;
            }
            if (cause == cause.getCause()) break;
        }
        return false;
    }

    private String acquireOperation() {
        String token = UUID.randomUUID().toString();
        // 崩溃恢复用租约；远大于底层 HTTP 请求超时，不等待以免接口线程堆积。
        if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(
                OPERATION_KEY, token, 30, TimeUnit.MINUTES))) {
            throw new JstPrintUnavailableException("聚水潭正在打印或校验身份，请稍后重试");
        }
        return token;
    }

    private void releaseOperation(String token) {
        redisTemplate.execute(RELEASE, Collections.singletonList(OPERATION_KEY), token);
    }
}

package com.mes.application.command.auth;

import com.mes.application.dto.req.auth.AddConfigUserRequest;
import com.mes.application.dto.req.auth.LoginRequest;
import com.mes.application.dto.resp.auth.LoginResponse;
import com.mes.domain.auth.entity.ConfigUser;
import com.mes.domain.auth.service.ConfigUserService;
import com.mes.domain.base.repository.ApiResponse;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AppConfigLoginService {

    private static final String CONFIG_LOGIN_TOKEN_KEY_PREFIX = "mes:config-auth:token:";

    @Autowired
    private ConfigUserService configUserService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${mes.login.token-valid-days:7}")
    private int tokenValidDays;

    public LoginResponse login(LoginRequest request) {
        ConfigUser user = configUserService.findByAccount(request.getAccount());
        if (user == null || !user.getPassword().equals(request.getPassword())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.unauthorized, "账号或密码错误");
        }

        String token = generateToken();
        Date expireAt = Date.from(Instant.now().plus(tokenValidDays, ChronoUnit.DAYS));

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserName(user.getName());
        response.setIsAdmin(Boolean.TRUE.equals(user.getIsAdmin()));
        response.setTokenExpireAt(expireAt);

        redisTemplate.opsForValue().set(buildLoginTokenCacheKey(token), user.getId(), tokenValidDays, TimeUnit.DAYS);
        return response;
    }

    public void addConfigUser(AddConfigUserRequest request) {
        ConfigUser user = new ConfigUser();
        user.setAccount(request.getAccount());
        user.setPassword(request.getPassword());
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setIsAdmin(Boolean.TRUE.equals(request.getIsAdmin()));
        configUserService.add(user);
    }

    public String getConfigUserIdByToken(String token) {
        Object configUserId = redisTemplate.opsForValue().get(buildLoginTokenCacheKey(token));
        if (configUserId == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.unauthorized, "token无效");
        }
        return String.valueOf(configUserId);
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") + Long.toHexString(System.currentTimeMillis());
    }

    private String buildLoginTokenCacheKey(String token) {
        return CONFIG_LOGIN_TOKEN_KEY_PREFIX + token;
    }
}

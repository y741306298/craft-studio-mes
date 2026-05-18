package com.mes.application.command.auth;

import com.mes.application.dto.req.auth.*;
import com.mes.application.dto.resp.auth.LoginResponse;
import com.mes.application.command.manufacturerMeta.AppManufacturerMetaService;
import com.mes.domain.auth.entity.ManufacturerUser;
import com.mes.domain.auth.service.ManufacturerUserService;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerMeta;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;

import java.util.List;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AppLoginService {

    private static final String LOGIN_TOKEN_KEY_PREFIX = "mes:auth:token:";

    @Autowired
    private ManufacturerUserService manufacturerUserService;
    @Autowired
    private AppManufacturerMetaService appManufacturerMetaService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${mes.login.token-valid-days:7}")
    private int tokenValidDays;

    public LoginResponse login(LoginRequest request) {
        ManufacturerUser user = manufacturerUserService.findByAccount(request.getAccount());
        if (user == null || !user.getPassword().equals(request.getPassword())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.unauthorized, "账号或密码错误");
        }

        String token = generateToken();
        Date expireAt = Date.from(Instant.now().plus(tokenValidDays, ChronoUnit.DAYS));

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setManufacturerMetaId(user.getManufacturerMetaId());
        ManufacturerMeta manufacturerMeta = appManufacturerMetaService.findByManufacturerMetaId(user.getManufacturerMetaId());
        response.setManufacturerMetaName(manufacturerMeta == null ? null : manufacturerMeta.getName());
        response.setUserName(user.getName());
        response.setIsAdmin(Boolean.TRUE.equals(user.getIsAdmin()));
        response.setTokenExpireAt(expireAt);

        redisTemplate.opsForValue().set(buildLoginTokenCacheKey(token), user.getManufacturerMetaId(), tokenValidDays, TimeUnit.DAYS);
        return response;
    }

    public String getManufacturerMetaIdByToken(String token) {
        Object manufacturerMetaId = redisTemplate.opsForValue().get(buildLoginTokenCacheKey(token));
        if (manufacturerMetaId == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.unauthorized, "token无效");
        }

        return String.valueOf(manufacturerMetaId);
    }

    public void addUser(AddUserRequest request) {
        ManufacturerUser user = new ManufacturerUser();
        user.setAccount(request.getAccount());
        user.setPassword(request.getPassword());
        user.setManufacturerMetaId(request.getManufacturerMetaId());
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setIsAdmin(Boolean.TRUE.equals(request.getIsAdmin()));
        manufacturerUserService.add(user);
    }

    public PagedResult<ManufacturerUser> listUsersByManufacturerMetaId(String manufacturerMetaId, String phone, PagedQuery query) {
        List<ManufacturerUser> items = manufacturerUserService.listByManufacturerMetaId(manufacturerMetaId, phone, query.getCurrent(), query.getSize());
        long total = manufacturerUserService.totalByManufacturerMetaId(manufacturerMetaId, phone);
        return new PagedResult<>(items, total, query.getSize(), query.getCurrent());
    }

    public void deleteUser(DeleteUserRequest request) {
        manufacturerUserService.deleteById(request.getId());
    }

    public void updateUserPassword(UpdateUserPasswordRequest request) {
        manufacturerUserService.updatePassword(request.getId(), request.getPassword());
    }

    /**
     * 更新用户信息
     * @param request 更新用户请求
     */
    public void updateUser(UpdateUserRequest request) {
        manufacturerUserService.updateUser(
            request.getId(),
            request.getName(),
            request.getPhone(),
            request.getIsAdmin()
        );
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") + Long.toHexString(System.currentTimeMillis());
    }

    private String buildLoginTokenCacheKey(String token) {
        return LOGIN_TOKEN_KEY_PREFIX + token;
    }
}

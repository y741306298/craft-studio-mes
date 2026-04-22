package com.mes.application.command.auth;

import com.mes.application.dto.req.auth.AddUserRequest;
import com.mes.application.dto.req.auth.LoginRequest;
import com.mes.application.dto.resp.auth.LoginResponse;
import com.mes.domain.auth.entity.ManufacturerUser;
import com.mes.domain.auth.service.ManufacturerUserService;
import com.mes.domain.base.repository.ApiResponse;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AppLoginService {

    private static final Map<String, LoginTokenInfo> LOGIN_TOKEN_STORE = new ConcurrentHashMap<>();

    @Autowired
    private ManufacturerUserService manufacturerUserService;

    @Value("${mes.login.token-valid-days:3}")
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
        response.setTokenExpireAt(expireAt);

        LOGIN_TOKEN_STORE.put(token, new LoginTokenInfo(user.getManufacturerMetaId(), expireAt));
        return response;
    }

    public String getManufacturerMetaIdByToken(String token) {
        LoginTokenInfo tokenInfo = LOGIN_TOKEN_STORE.get(token);
        if (tokenInfo == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.unauthorized, "token无效");
        }

        if (tokenInfo.getExpireAt().before(new Date())) {
            LOGIN_TOKEN_STORE.remove(token);
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.unauthorized, "token已过期");
        }

        return tokenInfo.getManufacturerMetaId();
    }

    public void addUser(AddUserRequest request) {
        ManufacturerUser user = new ManufacturerUser();
        user.setAccount(request.getAccount());
        user.setPassword(request.getPassword());
        user.setManufacturerMetaId(request.getManufacturerMetaId());
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        manufacturerUserService.add(user);
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") + Long.toHexString(System.currentTimeMillis());
    }

    private static class LoginTokenInfo {
        private final String manufacturerMetaId;
        private final Date expireAt;

        private LoginTokenInfo(String manufacturerMetaId, Date expireAt) {
            this.manufacturerMetaId = manufacturerMetaId;
            this.expireAt = expireAt;
        }

        public String getManufacturerMetaId() {
            return manufacturerMetaId;
        }

        public Date getExpireAt() {
            return expireAt;
        }
    }
}

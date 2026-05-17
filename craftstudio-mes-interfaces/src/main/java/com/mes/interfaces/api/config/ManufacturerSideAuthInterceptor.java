package com.mes.interfaces.api.config;

import com.mes.application.command.auth.AppLoginService;
import com.mes.domain.base.repository.ApiResponse;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ManufacturerSideAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AppLoginService appLoginService;

    public ManufacturerSideAuthInterceptor(AppLoginService appLoginService) {
        this.appLoginService = appLoginService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.unauthorized, "请先登录");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.unauthorized, "请先登录");
        }

        try {
            appLoginService.getManufacturerMetaIdByToken(token);
        } catch (BusinessNotAllowException ex) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.unauthorized, "登录已失效，请重新登录");
        }
        return true;
    }
}

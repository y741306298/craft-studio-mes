package com.mes.interfaces.api.config;

import com.alibaba.fastjson.JSON;
import com.mes.application.command.auth.AppLoginService;
import com.mes.domain.base.repository.ApiResponse;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
public class ManufacturerSideAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AppLoginService appLoginService;

    public ManufacturerSideAuthInterceptor(AppLoginService appLoginService) {
        this.appLoginService = appLoginService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            writeUnauthorizedResponse(request, response, "请先登录");
            return false;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            writeUnauthorizedResponse(request, response, "请先登录");
            return false;
        }

        try {
            appLoginService.getManufacturerMetaIdByToken(token);
        } catch (BusinessNotAllowException ex) {
            writeUnauthorizedResponse(request, response, "登录已失效，请重新登录");
            return false;
        }
        return true;
    }

    private void writeUnauthorizedResponse(HttpServletRequest request, HttpServletResponse response, String message) throws Exception {
        appendCorsHeaders(request, response);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<String> body = ApiResponse.fail(ApiResponse.RepStatusCode.unauthorized, message);
        response.getWriter().write(JSON.toJSONString(body));
        response.getWriter().flush();
    }

    private void appendCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            String requestHeaders = request.getHeader("Access-Control-Request-Headers");
            response.setHeader("Access-Control-Allow-Headers",
                    requestHeaders == null || requestHeaders.isBlank() ? "*" : requestHeaders);
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH");
        }
    }
}

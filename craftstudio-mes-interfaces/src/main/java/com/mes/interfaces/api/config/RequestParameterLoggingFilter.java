package com.mes.interfaces.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "mes.request-log", name = "enabled", havingValue = "true")
public class RequestParameterLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestParameterLoggingFilter.class);
    private static final int REQUEST_CACHE_LIMIT = 1024 * 1024;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = request instanceof ContentCachingRequestWrapper
                ? (ContentCachingRequestWrapper) request
                : new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT);

        filterChain.doFilter(wrappedRequest, response);

        try {
            log.info("HTTP请求入参 => method={}, uri={}, query={}, params={}, body={}",
                    wrappedRequest.getMethod(),
                    wrappedRequest.getRequestURI(),
                    wrappedRequest.getQueryString(),
                    buildParamLog(wrappedRequest.getParameterMap()),
                    buildBodyLog(wrappedRequest));
        } catch (Exception e) {
            log.warn("记录HTTP请求入参日志失败: method={}, uri={}", wrappedRequest.getMethod(), wrappedRequest.getRequestURI(), e);
        }
    }

    private String buildParamLog(Map<String, String[]> parameterMap) {
        if (parameterMap == null || parameterMap.isEmpty()) {
            return "{}";
        }
        return parameterMap.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + Arrays.toString(entry.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private String buildBodyLog(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content == null || content.length == 0) {
            return "";
        }
        Charset charset = StandardCharsets.UTF_8;
        String encoding = request.getCharacterEncoding();
        if (StringUtils.hasText(encoding)) {
            try {
                charset = Charset.forName(encoding);
            } catch (Exception ignored) {
                // fallback UTF-8
            }
        }
        return StringUtils.trimWhitespace(new String(content, charset));
    }

}
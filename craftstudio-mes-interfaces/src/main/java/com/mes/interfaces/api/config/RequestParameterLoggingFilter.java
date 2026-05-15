package com.mes.interfaces.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RequestParameterLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestParameterLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = request instanceof ContentCachingRequestWrapper
                ? (ContentCachingRequestWrapper) request
                : new ContentCachingRequestWrapper(request);

        filterChain.doFilter(wrappedRequest, response);

        log.info("HTTP请求入参 => method={}, uri={}, query={}, params={}, body={}",
                wrappedRequest.getMethod(),
                wrappedRequest.getRequestURI(),
                wrappedRequest.getQueryString(),
                buildParamLog(wrappedRequest.getParameterMap()),
                buildBodyLog(wrappedRequest));
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
        String charset = request.getCharacterEncoding();
        String body = new String(content, StringUtils.hasText(charset) ? java.nio.charset.Charset.forName(charset) : StandardCharsets.UTF_8);
        return StringUtils.trimWhitespace(body);
    }
}

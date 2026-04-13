package com.mes.interfaces.api.handler;

import com.mes.application.dto.resp.ApiResponse;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiControllerHandler {

    @ExceptionHandler(BusinessNotAllowException.class)
    public ApiResponse<String> handleBusinessNotAllowException(BusinessNotAllowException ex) {
        return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleException(Exception ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "服务异常" : ex.getMessage();
        return ApiResponse.fail(ApiResponse.RepStatusCode.serviceError, message);
    }
}

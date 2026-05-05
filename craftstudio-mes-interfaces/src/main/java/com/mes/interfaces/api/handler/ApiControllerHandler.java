package com.mes.interfaces.api.handler;

import com.mes.domain.base.repository.ApiResponse;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiControllerHandler {

    @ExceptionHandler(BusinessNotAllowException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handleBusinessNotAllowException(BusinessNotAllowException ex) {
        return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<String> handleException(Exception ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "服务异常" : ex.getMessage();
        return ApiResponse.fail(ApiResponse.RepStatusCode.serviceError, message);
    }
}

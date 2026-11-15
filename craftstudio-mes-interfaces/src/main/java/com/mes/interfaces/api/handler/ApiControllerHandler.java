package com.mes.interfaces.api.handler;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.application.command.delivery.JstPrintGuard;
import com.mes.application.command.delivery.JstPrintUnavailableException;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.auth.SmsCodeRequiredAuthException;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiControllerHandler {
    @ExceptionHandler(com.mes.application.command.graphics.GraphicsWorkflowException.class)
    public org.springframework.http.ResponseEntity<ApiResponse<String>> graphics(com.mes.application.command.graphics.GraphicsWorkflowException ex){
        var builder=org.springframework.http.ResponseEntity.status(ex.status());
        if(ex.status()==429)builder.header("Retry-After","5");
        return builder.body(ApiResponse.fail(ex.status(),ex.getMessage()));
    }

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

    @ExceptionHandler({SmsCodeRequiredAuthException.class, JstPrintUnavailableException.class})
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.LOCKED)
    public ApiResponse<String> handleJstPrintLocked(RuntimeException ex) {
        String message = ex instanceof SmsCodeRequiredAuthException
                ? JstPrintGuard.AUTH_REQUIRED_MESSAGE : ex.getMessage();
        return ApiResponse.fail(423, message);
    }

    @ExceptionHandler(Exception.class)
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<String> handleException(Exception ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "服务异常" : ex.getMessage();
        return ApiResponse.fail(ApiResponse.RepStatusCode.serviceError, message);
    }
}

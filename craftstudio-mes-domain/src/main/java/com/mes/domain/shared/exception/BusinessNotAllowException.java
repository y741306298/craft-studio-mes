package com.mes.domain.shared.exception;

public class BusinessNotAllowException extends RuntimeException {
    public BusinessNotAllowException(String message) {
        super(message);
    }
}

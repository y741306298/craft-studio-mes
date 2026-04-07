package com.mes.application.dto.req.base;

public abstract class ApiRequest {
    abstract public boolean isValid();
    abstract public String getValidationMessage();
}

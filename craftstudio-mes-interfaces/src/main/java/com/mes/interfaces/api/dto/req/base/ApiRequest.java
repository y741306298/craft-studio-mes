package com.mes.interfaces.api.dto.req.base;

public abstract class ApiRequest {
    abstract public boolean isValid();
    abstract public String getValidationMessage();
}

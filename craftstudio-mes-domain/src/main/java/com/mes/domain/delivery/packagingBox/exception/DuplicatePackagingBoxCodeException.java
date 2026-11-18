package com.mes.domain.delivery.packagingBox.exception;

public class DuplicatePackagingBoxCodeException extends IllegalArgumentException {
    public DuplicatePackagingBoxCodeException(Throwable cause) {
        super("包装箱编码(code)已存在", cause);
    }
}

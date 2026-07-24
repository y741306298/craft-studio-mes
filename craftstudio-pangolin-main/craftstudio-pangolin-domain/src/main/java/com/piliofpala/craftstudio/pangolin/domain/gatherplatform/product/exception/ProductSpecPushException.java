package com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.exception;

import com.piliofpala.craftstudio.shared.domain.base.exception.DomainException;

public class ProductSpecPushException extends DomainException {
    public ProductSpecPushException(String message) {
        super(message);
    }

    public ProductSpecPushException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.piliofpala.craftstudio.pangolin.domain.gatherplatform.shop.exception;

import com.piliofpala.craftstudio.shared.domain.base.exception.DomainException;

public class ShopNotUniqueException extends DomainException {
    public ShopNotUniqueException(String message) {
        super(message);
    }
}

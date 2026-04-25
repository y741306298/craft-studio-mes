package com.mes.domain.manufacturer.typesetting.enums;

public enum TypesettingPrintTaskStatus {
    PENDING("待领取"),
    CLAIMED("已领取");

    private final String code;

    TypesettingPrintTaskStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

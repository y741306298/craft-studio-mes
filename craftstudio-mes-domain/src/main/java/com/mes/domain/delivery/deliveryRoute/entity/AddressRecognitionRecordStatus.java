package com.mes.domain.delivery.deliveryRoute.entity;

public enum AddressRecognitionRecordStatus {
    ASSIGNED("已分配"),
    UNASSIGNED("未分配");

    private final String value;

    AddressRecognitionRecordStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AddressRecognitionRecordStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (AddressRecognitionRecordStatus status : values()) {
            if (status.name().equals(value) || status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }
}

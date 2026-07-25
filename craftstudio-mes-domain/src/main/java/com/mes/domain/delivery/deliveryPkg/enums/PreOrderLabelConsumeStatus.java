package com.mes.domain.delivery.deliveryPkg.enums;

/** Lifecycle of a pre-ordered label when it is consumed by formal packing. */
public enum PreOrderLabelConsumeStatus {
    PRE_ORDERED,
    PRINTING,
    CONSUMED,
    PRINT_FAILED
}

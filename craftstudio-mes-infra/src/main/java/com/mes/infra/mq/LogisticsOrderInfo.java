package com.mes.infra.mq;

@lombok.Data
public class LogisticsOrderInfo {
    public Long orderId;
    public String logisticsOrderId;
    public String state;
}

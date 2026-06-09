package com.mes.application.dto.req.delivery;

import lombok.Data;

import java.util.Date;

@Data
public class DeliveryPkgScopedRequest {

    private String manufacturerMetaId;
    private String orderId;
    private String orderItemId;
    private String customerPhone;
    private String customerName;
    private Date startTime;
    private Date endTime;
    private String carrierName;
    private String materialName;
    private String processName;
    private Double width;
}

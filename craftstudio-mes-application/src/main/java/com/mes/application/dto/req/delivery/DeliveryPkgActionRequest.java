package com.mes.application.dto.req.delivery;

import lombok.Data;

@Data
public class DeliveryPkgActionRequest {
    private String deliveryPkgId;
    /** 快递100云打印设备编码；为空时使用原包裹/打印记录的设备 */
    private String siid;
}

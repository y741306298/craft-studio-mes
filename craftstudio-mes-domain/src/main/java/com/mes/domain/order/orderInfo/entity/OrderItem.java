package com.mes.domain.order.orderInfo.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.vo.ManufacturerMtsProductSpec;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderItem extends BaseEntity {

    private String orderItemId;
    private String orderId;
    private ManufacturerMtsProductSpec mtsProduct;
    private String material;
    private String procedureFlowId;
    private Integer quantity;
    private String status;
    private Boolean isUrgent;
    private String processingFlow;
    private String productionImgUrl;
    private String maskImgUrl;


}

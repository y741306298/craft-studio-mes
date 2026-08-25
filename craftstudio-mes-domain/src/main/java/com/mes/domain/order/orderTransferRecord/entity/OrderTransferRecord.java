package com.mes.domain.order.orderTransferRecord.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderTransferRecord extends BaseEntity {

    private String orderId;
    private String sourceId;
    private String sourceName;
    private String targetId;
    private String targetName;
    private String orderItemId;
    /** Order-item ID generated at the target factory. */
    private String targetOrderItemId;
    private String previewUrl;
    private Integer quantity;
}

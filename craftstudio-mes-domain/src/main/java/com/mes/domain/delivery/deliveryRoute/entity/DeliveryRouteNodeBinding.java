package com.mes.domain.delivery.deliveryRoute.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeliveryRouteNodeBinding extends BaseEntity {

    private String manufacturerMetaId;
    private String terminalRegionCode;
    private String detailAddress;
    private String routeNodeId;
}

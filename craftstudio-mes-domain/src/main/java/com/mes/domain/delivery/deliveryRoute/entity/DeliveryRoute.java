package com.mes.domain.delivery.deliveryRoute.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
public class DeliveryRoute extends BaseEntity {

    private String routeId;
    private String routeName;
    private String manufacturerMetaId;
    private List<DeliveryRouteNode> deliveryRouteNodes;
    private String status;

}

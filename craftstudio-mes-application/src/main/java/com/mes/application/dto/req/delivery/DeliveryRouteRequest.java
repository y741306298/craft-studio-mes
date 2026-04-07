package com.mes.application.dto.req.delivery;

import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNode;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class DeliveryRouteRequest {

    private String id;

    @NotBlank(message = "路线 ID 不能为空")
    private String routeId;

    @NotBlank(message = "路线名称不能为空")
    private String routeName;

    private List<DeliveryRouteNode> deliveryRouteNodes;

    private String status;

    public DeliveryRoute toDomainEntity() {
        DeliveryRoute deliveryRoute = new DeliveryRoute();
        deliveryRoute.setId(this.id);
        deliveryRoute.setRouteId(this.routeId);
        deliveryRoute.setRouteName(this.routeName);
        deliveryRoute.setDeliveryRouteNodes(this.deliveryRouteNodes);
        deliveryRoute.setStatus(this.status);
        return deliveryRoute;
    }
}

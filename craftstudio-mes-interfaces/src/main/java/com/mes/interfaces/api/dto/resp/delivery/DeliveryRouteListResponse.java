package com.mes.interfaces.api.dto.resp.delivery;

import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNode;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class DeliveryRouteListResponse {

    private String id;
    private String routeId;
    private String routeName;
    private List<DeliveryRouteNode> deliveryRouteNodes;
    private String status;
    private Date createTime;
    private Date updateTime;

    public static DeliveryRouteListResponse from(DeliveryRoute deliveryRoute) {
        if (deliveryRoute == null) {
            return null;
        }

        DeliveryRouteListResponse response = new DeliveryRouteListResponse();
        response.setId(deliveryRoute.getId());
        response.setRouteId(deliveryRoute.getRouteId());
        response.setRouteName(deliveryRoute.getRouteName());
        response.setDeliveryRouteNodes(deliveryRoute.getDeliveryRouteNodes());
        response.setStatus(deliveryRoute.getStatus());
        response.setCreateTime(deliveryRoute.getCreateTime());
        response.setUpdateTime(deliveryRoute.getUpdateTime());

        return response;
    }
}

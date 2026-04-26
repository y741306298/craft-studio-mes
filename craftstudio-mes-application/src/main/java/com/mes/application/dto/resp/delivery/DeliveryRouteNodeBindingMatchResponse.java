package com.mes.application.dto.resp.delivery;

import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNode;
import lombok.Data;

@Data
public class DeliveryRouteNodeBindingMatchResponse {

    private String matchStatus;
    private String message;
    private DeliveryRoute deliveryRoute;
    private DeliveryRouteNode deliveryRouteNode;

    public static DeliveryRouteNodeBindingMatchResponse unmatched() {
        DeliveryRouteNodeBindingMatchResponse response = new DeliveryRouteNodeBindingMatchResponse();
        response.setMatchStatus("UNMATCHED");
        response.setMessage("未匹配");
        return response;
    }

    public static DeliveryRouteNodeBindingMatchResponse matched(DeliveryRoute route, DeliveryRouteNode node) {
        DeliveryRouteNodeBindingMatchResponse response = new DeliveryRouteNodeBindingMatchResponse();
        response.setMatchStatus("MATCHED");
        response.setMessage("已匹配");
        response.setDeliveryRoute(route);
        response.setDeliveryRouteNode(node);
        return response;
    }
}

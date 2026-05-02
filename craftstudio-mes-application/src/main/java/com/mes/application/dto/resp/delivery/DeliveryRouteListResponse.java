package com.mes.application.dto.resp.delivery;

import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNode;
import lombok.Data;

import java.util.Date;
import java.util.ArrayList;
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
        return from(deliveryRoute, true);
    }

    public static DeliveryRouteListResponse fromRaw(DeliveryRoute deliveryRoute) {
        return from(deliveryRoute, false);
    }

    private static DeliveryRouteListResponse from(DeliveryRoute deliveryRoute, boolean rebuildNodes) {
        if (deliveryRoute == null) {
            return null;
        }

        DeliveryRouteListResponse response = new DeliveryRouteListResponse();
        response.setId(deliveryRoute.getId());
        response.setRouteId(deliveryRoute.getRouteId());
        response.setRouteName(deliveryRoute.getRouteName());
        response.setDeliveryRouteNodes(rebuildNodes
                ? rebuildDeliveryRouteNodes(deliveryRoute.getDeliveryRouteNodes())
                : deliveryRoute.getDeliveryRouteNodes());
        response.setStatus(deliveryRoute.getStatus());
        response.setCreateTime(deliveryRoute.getCreateTime());
        response.setUpdateTime(deliveryRoute.getUpdateTime());

        return response;
    }

    private static List<DeliveryRouteNode> rebuildDeliveryRouteNodes(List<DeliveryRouteNode> storedNodes) {
        if (storedNodes == null || storedNodes.isEmpty()) {
            return storedNodes;
        }

        List<DeliveryRouteNode> rebuiltNodes = new ArrayList<>(storedNodes);
        DeliveryRouteNode lastStoredNode = storedNodes.get(storedNodes.size() - 1);
        if (lastStoredNode == null) {
            return rebuiltNodes;
        }

        DeliveryRouteNode generatedLastNode = new DeliveryRouteNode();
        generatedLastNode.setRouteId(lastStoredNode.getRouteId());
        generatedLastNode.setCountryCode(lastStoredNode.getDestCountryCode());
        generatedLastNode.setCountryName(lastStoredNode.getDestCountryName());
        generatedLastNode.setProvinceCode(lastStoredNode.getDestProvinceCode());
        generatedLastNode.setProvinceName(lastStoredNode.getDestProvinceName());
        generatedLastNode.setCityCode(lastStoredNode.getDestCityCode());
        generatedLastNode.setCityName(lastStoredNode.getDestCityName());
        generatedLastNode.setDistrictCode(lastStoredNode.getDestDistrictCode());
        generatedLastNode.setDistrictName(lastStoredNode.getDestDistrictName());
        generatedLastNode.setTownCode(lastStoredNode.getDestTownCode());
        generatedLastNode.setTownName(lastStoredNode.getDestTownName());
        generatedLastNode.setDetailAddress(lastStoredNode.getDestDetailAddress());
        generatedLastNode.setNodeType("END");
        generatedLastNode.setStatus(lastStoredNode.getStatus());
        generatedLastNode.setNodeOrder(lastStoredNode.getNodeOrder() == null ? rebuiltNodes.size() : lastStoredNode.getNodeOrder() + 1);
        generatedLastNode.buildRegionPath();

        rebuiltNodes.add(generatedLastNode);
        return rebuiltNodes;
    }
}

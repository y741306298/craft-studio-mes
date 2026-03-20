package com.mes.interfaces.api.dto.req.delivery;

import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNode;
import lombok.Data;

@Data
public class DeliveryRouteNodeRequest {

    private String routeNodeId;
    private String countryCode;
    private String countryName;
    private String provinceCode;
    private String provinceName;
    private String cityCode;
    private String cityName;
    private String districtCode;
    private String districtName;
    private String townCode;
    private String townName;
    private String regionPath;
    private String status;
    private String nodeType;
    private Integer nodeOrder;
    private String detailAddress;

    public DeliveryRouteNode toDomainEntity() {
        DeliveryRouteNode node = new DeliveryRouteNode();
        node.setRouteNodeId(this.routeNodeId);
        node.setCountryCode(this.countryCode);
        node.setCountryName(this.countryName);
        node.setProvinceCode(this.provinceCode);
        node.setProvinceName(this.provinceName);
        node.setCityCode(this.cityCode);
        node.setCityName(this.cityName);
        node.setDistrictCode(this.districtCode);
        node.setDistrictName(this.districtName);
        node.setTownCode(this.townCode);
        node.setTownName(this.townName);
        node.setRegionPath(this.regionPath);
        node.setStatus(this.status);
        node.setNodeType(this.nodeType);
        node.setNodeOrder(this.nodeOrder);
        node.setDetailAddress(this.detailAddress);
        return node;
    }
}

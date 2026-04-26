package com.mes.application.dto.resp.delivery;

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
    private String destCountryCode;
    private String destCountryName;
    private String destProvinceCode;
    private String destProvinceName;
    private String destCityCode;
    private String destCityName;
    private String destDistrictCode;
    private String destDistrictName;
    private String destTownCode;
    private String destTownName;
    private String destRegionPath;
    private String status;
    private String nodeType;
    private Integer nodeOrder;
    private String detailAddress;
    private String destDetailAddress;

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
        node.setDestCountryCode(this.destCountryCode);
        node.setDestCountryName(this.destCountryName);
        node.setDestProvinceCode(this.destProvinceCode);
        node.setDestProvinceName(this.destProvinceName);
        node.setDestCityCode(this.destCityCode);
        node.setDestCityName(this.destCityName);
        node.setDestDistrictCode(this.destDistrictCode);
        node.setDestDistrictName(this.destDistrictName);
        node.setDestTownCode(this.destTownCode);
        node.setDestTownName(this.destTownName);
        node.setDestRegionPath(this.destRegionPath);
        node.setStatus(this.status);
        node.setNodeType(this.nodeType);
        node.setNodeOrder(this.nodeOrder);
        node.setDetailAddress(this.detailAddress);
        node.setDestDetailAddress(this.destDetailAddress);
        return node;
    }
}

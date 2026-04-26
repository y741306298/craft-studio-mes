package com.mes.infra.dal.delivery.deliveryRoute.po;

import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNode;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "deliveryRouteNode")
public class DeliveryRouteNodePo extends BasePO<DeliveryRouteNode> {

    private String routeNodeId;
    private String routeId;

    // 地理区域信息（来自 craftstudio-shared 的 geo.world 域）
    private String countryCode; // 国家编码
    private String countryName; // 国家名称
    private String provinceCode; // 省份编码
    private String provinceName; // 省份名称
    private String cityCode; // 城市编码
    private String cityName; // 城市名称
    private String districtCode; // 区县编码
    private String districtName; // 区县名称
    private String townCode; // 乡镇编码
    private String townName; // 乡镇名称
    // 起点完整的地区路径，如：中国/广东省/深圳市/南山区
    private String regionPath;
    private String destCountryCode; // 终点国家编码
    private String destCountryName; // 终点国家名称
    private String destProvinceCode; // 终点省份编码
    private String destProvinceName; // 终点省份名称
    private String destCityCode; // 终点城市编码
    private String destCityName; // 终点城市名称
    private String destDistrictCode; // 终点区县编码
    private String destDistrictName; // 终点区县名称
    private String destTownCode; // 终点乡镇编码
    private String destTownName; // 终点乡镇名称
    // 终点完整地区路径
    private String destRegionPath;
    // 节点状态：ACTIVE-激活，INACTIVE-未激活
    private String status;
    // 节点类型：START-起点，TRANSIT-中转点，END-终点
    private String nodeType;
    // 节点顺序（在路线中的位置）
    private Integer nodeOrder;
    // 起点详细地址
    private String detailAddress;
    // 终点详细地址
    private String destDetailAddress;



    @Override
    public DeliveryRouteNode toDO() {
        DeliveryRouteNode routeNode = new DeliveryRouteNode();
        copyBaseFieldsToDO(routeNode);
        
        routeNode.setRouteNodeId(this.routeNodeId);
        routeNode.setRouteId(this.routeId);
        routeNode.setCountryCode(this.countryCode);
        routeNode.setCountryName(this.countryName);
        routeNode.setProvinceCode(this.provinceCode);
        routeNode.setProvinceName(this.provinceName);
        routeNode.setCityCode(this.cityCode);
        routeNode.setCityName(this.cityName);
        routeNode.setDistrictCode(this.districtCode);
        routeNode.setDistrictName(this.districtName);
        routeNode.setTownCode(this.townCode);
        routeNode.setTownName(this.townName);
        routeNode.setRegionPath(this.regionPath);
        routeNode.setDestCountryCode(this.destCountryCode);
        routeNode.setDestCountryName(this.destCountryName);
        routeNode.setDestProvinceCode(this.destProvinceCode);
        routeNode.setDestProvinceName(this.destProvinceName);
        routeNode.setDestCityCode(this.destCityCode);
        routeNode.setDestCityName(this.destCityName);
        routeNode.setDestDistrictCode(this.destDistrictCode);
        routeNode.setDestDistrictName(this.destDistrictName);
        routeNode.setDestTownCode(this.destTownCode);
        routeNode.setDestTownName(this.destTownName);
        routeNode.setDestRegionPath(this.destRegionPath);
        routeNode.setStatus(this.status);
        routeNode.setNodeType(this.nodeType);
        routeNode.setNodeOrder(this.nodeOrder);
        routeNode.setDetailAddress(this.detailAddress);
        routeNode.setDestDetailAddress(this.destDetailAddress);
        return routeNode;
    }

    @Override
    protected BasePO<DeliveryRouteNode> fromDO(DeliveryRouteNode _do) {
        if (_do == null) {
            return null;
        }
        
        this.routeNodeId = _do.getRouteNodeId();
        this.routeId = _do.getRouteId();
        this.countryCode = _do.getCountryCode();
        this.countryName = _do.getCountryName();
        this.provinceCode = _do.getProvinceCode();
        this.provinceName = _do.getProvinceName();
        this.cityCode = _do.getCityCode();
        this.cityName = _do.getCityName();
        this.districtCode = _do.getDistrictCode();
        this.districtName = _do.getDistrictName();
        this.townCode = _do.getTownCode();
        this.townName = _do.getTownName();
        this.regionPath = _do.getRegionPath();
        this.destCountryCode = _do.getDestCountryCode();
        this.destCountryName = _do.getDestCountryName();
        this.destProvinceCode = _do.getDestProvinceCode();
        this.destProvinceName = _do.getDestProvinceName();
        this.destCityCode = _do.getDestCityCode();
        this.destCityName = _do.getDestCityName();
        this.destDistrictCode = _do.getDestDistrictCode();
        this.destDistrictName = _do.getDestDistrictName();
        this.destTownCode = _do.getDestTownCode();
        this.destTownName = _do.getDestTownName();
        this.destRegionPath = _do.getDestRegionPath();
        this.status = _do.getStatus();
        this.nodeType = _do.getNodeType();
        this.nodeOrder = _do.getNodeOrder();
        this.detailAddress = _do.getDetailAddress();
        this.destDetailAddress = _do.getDestDetailAddress();

        return this;
    }
}

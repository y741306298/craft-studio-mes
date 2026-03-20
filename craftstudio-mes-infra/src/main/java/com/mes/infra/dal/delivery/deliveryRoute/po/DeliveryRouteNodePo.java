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
    // 完整的地区路径，如：中国/广东省/深圳市/南山区
    private String regionPath;
    // 节点状态：ACTIVE-激活，INACTIVE-未激活
    private String status;
    // 节点类型：START-起点，TRANSIT-中转点，END-终点
    private String nodeType;
    // 节点顺序（在路线中的位置）
    private Integer nodeOrder;
    // 详细地址
    private String detailAddress;



    @Override
    public DeliveryRouteNode toDO() {
        DeliveryRouteNode routeNode = new DeliveryRouteNode();
        copyBaseFieldsToDO(routeNode);
        
        routeNode.setRouteNodeId(this.routeNodeId);
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
        routeNode.setStatus(this.status);
        routeNode.setNodeType(this.nodeType);
        routeNode.setNodeOrder(this.nodeOrder);
        routeNode.setDetailAddress(this.detailAddress);
        return routeNode;
    }

    @Override
    protected BasePO<DeliveryRouteNode> fromDO(DeliveryRouteNode _do) {
        if (_do == null) {
            return null;
        }
        
        this.routeNodeId = _do.getRouteNodeId();
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
        this.status = _do.getStatus();
        this.nodeType = _do.getNodeType();
        this.nodeOrder = _do.getNodeOrder();
        this.detailAddress = _do.getDetailAddress();

        return this;
    }
}

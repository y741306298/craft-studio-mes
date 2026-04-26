package com.mes.domain.delivery.deliveryRoute.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeliveryRouteNode extends BaseEntity {

    private String routeNodeId;
    private String routeId;
    
    // 起点地理区域信息（来自 craftstudio-shared 的 geo.world 域）
    private String countryCode; // 起点国家编码
    private String countryName; // 起点国家名称
    private String provinceCode; // 起点省份编码
    private String provinceName; // 起点省份名称
    private String cityCode; // 起点城市编码
    private String cityName; // 起点城市名称
    private String districtCode; // 起点区县编码
    private String districtName; // 起点区县名称
    private String townCode; // 起点乡镇编码
    private String townName; // 起点乡镇名称
    
    // 起点完整地区路径，如：中国/广东省/深圳市/南山区
    private String regionPath;

    // 终点地理区域信息（来自 craftstudio-shared 的 geo.world 域）
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
    // 终点完整地区路径，如：中国/广东省/深圳市/南山区
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

    /**
     * 获取完整的地区名称
     */
    public String getFullRegionName() {
        return getStartFullRegionName();
    }

    public String getStartFullRegionName() {
        StringBuilder sb = new StringBuilder();
        if (countryName != null && !countryName.isEmpty()) {
            sb.append(countryName);
        }
        if (provinceName != null && !provinceName.isEmpty()) {
            sb.append(provinceName);
        }
        if (cityName != null && !cityName.isEmpty()) {
            sb.append(cityName);
        }
        if (districtName != null && !districtName.isEmpty()) {
            sb.append(districtName);
        }
        if (townName != null && !townName.isEmpty()) {
            sb.append(townName);
        }
        return sb.toString();
    }

    public String getDestFullRegionName() {
        StringBuilder sb = new StringBuilder();
        if (destCountryName != null && !destCountryName.isEmpty()) {
            sb.append(destCountryName);
        }
        if (destProvinceName != null && !destProvinceName.isEmpty()) {
            sb.append(destProvinceName);
        }
        if (destCityName != null && !destCityName.isEmpty()) {
            sb.append(destCityName);
        }
        if (destDistrictName != null && !destDistrictName.isEmpty()) {
            sb.append(destDistrictName);
        }
        if (destTownName != null && !destTownName.isEmpty()) {
            sb.append(destTownName);
        }
        return sb.toString();
    }
    
    /**
     * 构建地区路径
     */
    public void buildRegionPath() {
        this.regionPath = buildSingleRegionPath(countryName, provinceName, cityName, districtName, townName);
        this.destRegionPath = buildSingleRegionPath(destCountryName, destProvinceName, destCityName, destDistrictName, destTownName);
    }

    private String buildSingleRegionPath(String country, String province, String city, String district, String town) {
        StringBuilder sb = new StringBuilder();
        if (country != null && !country.isEmpty()) {
            sb.append(country).append("/");
        }
        if (province != null && !province.isEmpty()) {
            sb.append(province).append("/");
        }
        if (city != null && !city.isEmpty()) {
            sb.append(city).append("/");
        }
        if (district != null && !district.isEmpty()) {
            sb.append(district).append("/");
        }
        if (town != null && !town.isEmpty()) {
            sb.append(town);
        }
        
        String path = sb.toString();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
    
    /**
     * 验证地区编码是否完整
     */
    public boolean validateRegionCodes() {
        return isNotBlank(countryCode) && isNotBlank(destCountryCode);
    }
    
    /**
     * 判断是否为起始节点
     */
    public boolean isStartNode() {
        return "START".equals(this.nodeType);
    }
    
    /**
     * 判断是否为中转节点
     */
    public boolean isTransitNode() {
        return "TRANSIT".equals(this.nodeType);
    }
    
    /**
     * 判断是否为结束节点
     */
    public boolean isEndNode() {
        return "END".equals(this.nodeType);
    }
    
    /**
     * 判断节点是否激活
     */
    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }
    
    /**
     * 验证节点信息是否完整
     */
    public boolean validateNodeInfo() {
        
        // 必须有节点类型
        if (nodeType == null || nodeType.trim().isEmpty()) {
            return false;
        }
        
        // 必须有节点顺序
        if (nodeOrder == null) {
            return false;
        }
        
        // 地区编码必须有效
        if (!validateRegionCodes()) {
            return false;
        }
        
        return true;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

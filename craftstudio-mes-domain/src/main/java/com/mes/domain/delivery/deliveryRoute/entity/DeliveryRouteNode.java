package com.mes.domain.delivery.deliveryRoute.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeliveryRouteNode extends BaseEntity {

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

    /**
     * 获取完整的地区名称
     */
    public String getFullRegionName() {
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
    
    /**
     * 构建地区路径
     */
    public void buildRegionPath() {
        StringBuilder sb = new StringBuilder();
        if (countryName != null && !countryName.isEmpty()) {
            sb.append(countryName).append("/");
        }
        if (provinceName != null && !provinceName.isEmpty()) {
            sb.append(provinceName).append("/");
        }
        if (cityName != null && !cityName.isEmpty()) {
            sb.append(cityName).append("/");
        }
        if (districtName != null && !districtName.isEmpty()) {
            sb.append(districtName).append("/");
        }
        if (townName != null && !townName.isEmpty()) {
            sb.append(townName);
        }
        
        String path = sb.toString();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        this.regionPath = path;
    }
    
    /**
     * 验证地区编码是否完整
     */
    public boolean validateRegionCodes() {
        // 至少需要国家编码
        if (countryCode == null || countryCode.trim().isEmpty()) {
            return false;
        }
        return true;
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
}

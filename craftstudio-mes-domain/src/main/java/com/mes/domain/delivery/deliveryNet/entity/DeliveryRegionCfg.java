package com.mes.domain.delivery.deliveryNet.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeliveryRegionCfg extends BaseEntity {

    private String regionCfgId;
    private String cfgName; // 配置名称
    
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
    
    private String status;
    
    // 备注
    private String remarks;
    
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
     * 判断是否启用
     */
    public boolean isEnabled() {
        return "ENABLED".equals(this.status);
    }
}

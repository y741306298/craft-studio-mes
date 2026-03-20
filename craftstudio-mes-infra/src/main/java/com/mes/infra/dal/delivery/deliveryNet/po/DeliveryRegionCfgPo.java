package com.mes.infra.dal.delivery.deliveryNet.po;

import com.mes.domain.delivery.deliveryNet.entity.DeliveryRegionCfg;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeliveryRegionCfgPo extends BasePO<DeliveryRegionCfg> {
    
    private String regionCfgId;
    private String cfgName;
    
    // 地理区域信息
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
    private String remarks;

    @Override
    public DeliveryRegionCfg toDO() {
        DeliveryRegionCfg regionCfg = new DeliveryRegionCfg();
        copyBaseFieldsToDO(regionCfg);
        
        regionCfg.setRegionCfgId(this.regionCfgId);
        regionCfg.setCfgName(this.cfgName);
        regionCfg.setCountryCode(this.countryCode);
        regionCfg.setCountryName(this.countryName);
        regionCfg.setProvinceCode(this.provinceCode);
        regionCfg.setProvinceName(this.provinceName);
        regionCfg.setCityCode(this.cityCode);
        regionCfg.setCityName(this.cityName);
        regionCfg.setDistrictCode(this.districtCode);
        regionCfg.setDistrictName(this.districtName);
        regionCfg.setTownCode(this.townCode);
        regionCfg.setTownName(this.townName);
        regionCfg.setRegionPath(this.regionPath);
        regionCfg.setStatus(this.status);
        regionCfg.setRemarks(this.remarks);
        
        return regionCfg;
    }

    @Override
    protected BasePO<DeliveryRegionCfg> fromDO(DeliveryRegionCfg _do) {
        if (_do == null) {
            return null;
        }
        
        this.regionCfgId = _do.getRegionCfgId();
        this.cfgName = _do.getCfgName();
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
        this.remarks = _do.getRemarks();
        
        return this;
    }
}

package com.mes.application.dto.req.manufacturerMeta;

import com.mes.application.dto.req.base.ApiRequest;
import com.mes.domain.manufacturer.device.enums.DeviceType;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.mes.domain.shared.enums.ProductUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ManufacturerDeviceCfgRequest extends ApiRequest {
    
    private String id;
    
    @NotBlank(message = "制造商 ID 不能为空")
    @Size(max = 50, message = "制造商 ID 长度不能超过 50 个字符")
    private String manufacturerMetaId;
    
    @NotBlank(message = "设备 ID 不能为空")
    @Size(max = 50, message = "设备 ID 长度不能超过 50 个字符")
    private String deviceInfoId;
    
    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称长度不能超过 100 个字符")
    private String deviceName;
    
    @NotBlank(message = "设备类型不能为空")
    @Size(max = 50, message = "设备类型长度不能超过 50 个字符")
    private String deviceType;
    
    @Size(max = 50, message = "设备编号长度不能超过 50 个字符")
    private String deviceCode;
    
    private Double capacity;
    
    @Size(max = 50, message = "产能单位长度不能超过 50 个字符")
    private String capacityUnit;

    private boolean bound;

    @Size(max = 50, message = "绑定版本长度不能超过 50 个字符")
    private String boundVersion;

    @Override
    public boolean isValid() {
        if (manufacturerMetaId == null || manufacturerMetaId.trim().isEmpty()) {
            return false;
        }
        if (deviceInfoId == null || deviceInfoId.trim().isEmpty()) {
            return false;
        }
        if (deviceName == null || deviceName.trim().isEmpty()) {
            return false;
        }
        if (deviceType == null || deviceType.trim().isEmpty()) {
            return false;
        }
        if (manufacturerMetaId.length() > 50) {
            return false;
        }
        if (deviceInfoId.length() > 50) {
            return false;
        }
        if (deviceName.length() > 100) {
            return false;
        }
        if (deviceType.length() > 50) {
            return false;
        }
        if (deviceCode != null && deviceCode.length() > 50) {
            return false;
        }
        if (capacityUnit != null && capacityUnit.length() > 50) {
            return false;
        }
        if (boundVersion != null && boundVersion.length() > 50) {
            return false;
        }
        return true;
    }

    @Override
    public String getValidationMessage() {
        if (manufacturerMetaId == null || manufacturerMetaId.trim().isEmpty()) {
            return "制造商 ID 不能为空";
        }
        if (deviceInfoId == null || deviceInfoId.trim().isEmpty()) {
            return "设备 ID 不能为空";
        }
        if (deviceName == null || deviceName.trim().isEmpty()) {
            return "设备名称不能为空";
        }
        if (deviceType == null || deviceType.trim().isEmpty()) {
            return "设备类型不能为空";
        }
        if (manufacturerMetaId.length() > 50) {
            return "制造商 ID 长度不能超过 50 个字符";
        }
        if (deviceInfoId.length() > 50) {
            return "设备 ID 长度不能超过 50 个字符";
        }
        if (deviceName.length() > 100) {
            return "设备名称长度不能超过 100 个字符";
        }
        if (deviceType.length() > 50) {
            return "设备类型长度不能超过 50 个字符";
        }
        if (deviceCode != null && deviceCode.length() > 50) {
            return "设备编号长度不能超过 50 个字符";
        }
        if (capacityUnit != null && capacityUnit.length() > 50) {
            return "产能单位长度不能超过 50 个字符";
        }
        if (boundVersion != null && boundVersion.length() > 50) {
            return "绑定版本长度不能超过 50 个字符";
        }
        return "";
    }

    public ManufacturerDeviceCfg toDomainEntity() {
        ManufacturerDeviceCfg deviceCfg = new ManufacturerDeviceCfg();
        deviceCfg.setId(this.id);
        deviceCfg.setManufacturerMetaId(this.manufacturerMetaId);
        deviceCfg.setDeviceInfoId(this.deviceInfoId);
        deviceCfg.setDeviceName(this.deviceName);
        deviceCfg.setDeviceType(DeviceType.getByCode(this.deviceType));
        deviceCfg.setDeviceCode(this.deviceCode);
        deviceCfg.setCapacity(this.capacity);
        deviceCfg.setCapacityUnit(ProductUnit.getByChineseName(this.capacityUnit));
        deviceCfg.setBound(this.bound);
        deviceCfg.setBoundVersion(this.boundVersion);
        return deviceCfg;
    }
}

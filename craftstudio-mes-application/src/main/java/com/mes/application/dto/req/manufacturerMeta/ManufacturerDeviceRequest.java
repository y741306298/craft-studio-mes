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
public class ManufacturerDeviceRequest extends ApiRequest {
    
    @NotBlank(message = "设备 ID 不能为空")
    @Size(max = 50, message = "设备 ID 长度不能超过 50 个字符")
    private String deviceInfoId;
    
    @Size(max = 100, message = "设备名称长度不能超过 100 个字符")
    private String deviceName;
    
    @Size(max = 50, message = "设备类型长度不能超过 50 个字符")
    private String deviceType;
    
    @Size(max = 200, message = "设备描述长度不能超过 200 个字符")
    private String description;
    
    @Size(max = 100, message = "设备型号长度不能超过 100 个字符")
    private String model;
    
    @Size(max = 50, message = "设备状态长度不能超过 50 个字符")
    private String status;
    
    @Size(max = 50, message = "设备编号长度不能超过 50 个字符")
    private String deviceCode;
    
    private Double capacity;
    
    @Size(max = 50, message = "产能单位长度不能超过 50 个字符")
    private String capacityUnit;

    /**
     * 转换为领域实体
     * @return DeviceInfo 领域实体
     */
    public ManufacturerDeviceCfg toDomainEntity() {
        ManufacturerDeviceCfg deviceCfg = new ManufacturerDeviceCfg();
        deviceCfg.setId(this.deviceInfoId);
        deviceCfg.setDeviceInfoId(this.deviceInfoId);
        deviceCfg.setDeviceName(this.deviceName);
        deviceCfg.setDeviceType(DeviceType.getByCode(this.deviceType));
        deviceCfg.setDeviceCode(this.deviceCode);
        deviceCfg.setCapacity(this.capacity);
        deviceCfg.setCapacityUnit(ProductUnit.getBySymbol(this.capacityUnit));
        return deviceCfg;
    }

    @Override
    public boolean isValid() {
        if (deviceInfoId == null || deviceInfoId.trim().isEmpty()) {
            return false;
        }
        if (deviceInfoId.length() > 50) {
            return false;
        }
        if (deviceName != null && deviceName.length() > 100) {
            return false;
        }
        if (deviceType != null && deviceType.length() > 50) {
            return false;
        }
        if (description != null && description.length() > 200) {
            return false;
        }
        if (model != null && model.length() > 100) {
            return false;
        }
        if (status != null && status.length() > 50) {
            return false;
        }
        return true;
    }

    @Override
    public String getValidationMessage() {
        if (deviceInfoId == null || deviceInfoId.trim().isEmpty()) {
            return "设备ID不能为空";
        }
        if (deviceInfoId.length() > 50) {
            return "设备ID长度不能超过50个字符";
        }
        if (deviceName != null && deviceName.length() > 100) {
            return "设备名称长度不能超过100个字符";
        }
        if (deviceType != null && deviceType.length() > 50) {
            return "设备类型长度不能超过50个字符";
        }
        if (description != null && description.length() > 200) {
            return "设备描述长度不能超过200个字符";
        }
        if (model != null && model.length() > 100) {
            return "设备型号长度不能超过100个字符";
        }
        if (status != null && status.length() > 50) {
            return "设备状态长度不能超过50个字符";
        }
        return "";
    }
}

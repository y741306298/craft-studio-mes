package com.mes.application.dto.req.device;

import com.mes.domain.manufacturer.device.entity.Device;
import com.mes.domain.manufacturer.device.entity.DeviceMaterial;
import com.mes.domain.manufacturer.device.entity.DeviceProcedure;
import com.mes.domain.manufacturer.device.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class DeviceRequest {

    private String id;

    private String deviceInfoId;

    @NotBlank(message = "设备名称不能为空")
    private String deviceInfoName;

    private String deviceType;

    private String capacity;

    private String unit;

    private String brand;

    private Double maxWeight;

    private Double maxHegiht;

    private String materials;

    private List<DeviceProcedure> deviceProcedures;

    private List<DeviceMaterial> deviceMaterials;

    public Device toDomainEntity() {
        Device device = new Device();
        device.setId(this.id);
        device.setDeviceInfoId(this.deviceInfoId);
        device.setDeviceInfoName(this.deviceInfoName);
        device.setDeviceType(DeviceType.getByCode(this.deviceType));
        device.setCapacity(this.capacity);
        device.setUnit(this.unit);
        device.setBrand(this.brand);
        device.setMaxWeight(this.maxWeight);
        device.setMaxHegiht(this.maxHegiht);
        device.setMaterials(this.materials);
        device.setDeviceProcedures(this.deviceProcedures);
        device.setDeviceMaterials(this.deviceMaterials);
        return device;
    }
}

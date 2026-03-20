package com.mes.infra.dal.manufacurer.device.po;

import com.mes.domain.manufacturer.device.entity.Device;
import com.mes.domain.manufacturer.device.entity.DeviceMaterial;
import com.mes.domain.manufacturer.device.entity.DeviceProcedure;
import com.mes.domain.manufacturer.device.enums.DeviceType;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "device")
public class DevicePo extends BasePO<Device> {
    private String deviceInfoId;
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

    @Override
    public Device toDO() {
        Device deviceInfo = new Device();
        deviceInfo.setId(getId());
        deviceInfo.setDeviceInfoId(deviceInfoId);
        deviceInfo.setDeviceInfoName(deviceInfoName);
        deviceInfo.setDeviceType(DeviceType.getByCode(deviceType));
        deviceInfo.setCapacity(capacity);
        deviceInfo.setUnit(unit);
        deviceInfo.setBrand(brand);
        deviceInfo.setMaxWeight(maxWeight);
        deviceInfo.setMaxHegiht(maxHegiht);
        deviceInfo.setMaterials(materials);
        deviceInfo.setDeviceProcedures(deviceProcedures);
        deviceInfo.setDeviceMaterials(deviceMaterials);
        return deviceInfo;
    }

    @Override
    protected BasePO<Device> fromDO(Device _do) {
        if (_do == null) {
            return null;
        }
        // 设置业务字段
        this.deviceInfoId = _do.getDeviceInfoId();
        this.deviceInfoName = _do.getDeviceInfoName();
        this.deviceType = _do.getDeviceType() != null ? _do.getDeviceType().getCode() : null;
        this.capacity = _do.getCapacity();
        this.unit = _do.getUnit();
        this.brand = _do.getBrand();
        this.maxWeight = _do.getMaxWeight();
        this.maxHegiht = _do.getMaxHegiht();
        this.materials = _do.getMaterials();
        this.deviceProcedures = _do.getDeviceProcedures();
        this.deviceMaterials = _do.getDeviceMaterials();
        return this;
    }
}

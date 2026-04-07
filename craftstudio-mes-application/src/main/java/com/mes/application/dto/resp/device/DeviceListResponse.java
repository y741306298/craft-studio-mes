package com.mes.application.dto.resp.device;

import com.mes.domain.manufacturer.device.entity.Device;
import com.mes.domain.manufacturer.device.entity.DeviceMaterial;
import com.mes.domain.manufacturer.device.entity.DeviceProcedure;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class DeviceListResponse {

    private String id;
    private String deviceInfoId;
    private String deviceInfoName;
    private String deviceType;
    private String deviceTypeName;
    private String capacity;
    private String unit;
    private String brand;
    private Double maxWeight;
    private Double maxHegiht;
    private String materials;
    private List<DeviceProcedure> deviceProcedures;
    private List<DeviceMaterial> deviceMaterials;
    private Date createTime;
    private Date updateTime;

    public static DeviceListResponse from(Device device) {
        if (device == null) {
            return null;
        }

        DeviceListResponse response = new DeviceListResponse();
        response.setId(device.getId());
        response.setDeviceInfoId(device.getDeviceInfoId());
        response.setDeviceInfoName(device.getDeviceInfoName());
        response.setDeviceType(device.getDeviceType() != null ? device.getDeviceType().getCode() : null);
        response.setDeviceTypeName(device.getDeviceType() != null ? device.getDeviceType().getChineseName() : null);
        response.setCapacity(device.getCapacity());
        response.setUnit(device.getUnit());
        response.setBrand(device.getBrand());
        response.setMaxWeight(device.getMaxWeight());
        response.setMaxHegiht(device.getMaxHegiht());
        response.setMaterials(device.getMaterials());
        response.setDeviceProcedures(device.getDeviceProcedures());
        response.setDeviceMaterials(device.getDeviceMaterials());
        response.setCreateTime(device.getCreateTime());
        response.setUpdateTime(device.getUpdateTime());

        return response;
    }
}

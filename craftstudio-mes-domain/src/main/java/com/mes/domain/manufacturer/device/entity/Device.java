package com.mes.domain.manufacturer.device.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.device.enums.DeviceType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class Device extends BaseEntity {

    private String deviceInfoId;
    private String deviceInfoName;
    private DeviceType deviceType;
    private String capacity;
    private String unit;
    private String brand;
    private Double maxWeight;
    private Double maxHegiht;
    private String materials;
    private List<DeviceProcedure> deviceProcedures;
    private List<DeviceMaterial> deviceMaterials;
}


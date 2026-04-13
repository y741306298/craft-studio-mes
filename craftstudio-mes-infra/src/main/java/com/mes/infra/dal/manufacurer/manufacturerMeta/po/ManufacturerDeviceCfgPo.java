package com.mes.infra.dal.manufacurer.manufacturerMeta.po;

import com.mes.domain.manufacturer.device.enums.DeviceType;
import com.mes.domain.manufacturer.enums.CfgStatus;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.mes.domain.shared.enums.ProductUnit;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "manufacturerDeviceCfg")
public class ManufacturerDeviceCfgPo extends BasePO<ManufacturerDeviceCfg> {

    private String manufacturerMetaId;          // 所属制造商 ID
    private String deviceId;                    // 设备 ID
    private String deviceName;                  // 设备名称
    private String deviceType;                  // 设备类型 code
    private String deviceCode;                  // 设备编号
    private Double capacity;                    // 产能
    private String capacityUnit;                // 产能单位 code
    private String status;                      // 设备状态 code

    @Override
    public ManufacturerDeviceCfg toDO() {
        ManufacturerDeviceCfg deviceCfg = new ManufacturerDeviceCfg();
        deviceCfg.setId(getId());
        copyBaseFieldsToDO(deviceCfg);
        deviceCfg.setManufacturerMetaId(manufacturerMetaId);
        deviceCfg.setDeviceId(deviceId);
        deviceCfg.setDeviceName(deviceName);
        deviceCfg.setDeviceType(DeviceType.getByCode(deviceType));
        deviceCfg.setDeviceCode(deviceCode);
        deviceCfg.setCapacity(capacity);
        deviceCfg.setCapacityUnit(ProductUnit.getByChineseName(capacityUnit));
        deviceCfg.setStatus(CfgStatus.getByCode(status));
        return deviceCfg;
    }

    @Override
    protected BasePO<ManufacturerDeviceCfg> fromDO(ManufacturerDeviceCfg _do) {
        if (_do == null) {
            return null;
        }
        this.manufacturerMetaId = _do.getManufacturerMetaId();
        this.deviceId = _do.getDeviceId();
        this.deviceName = _do.getDeviceName();
        this.deviceType = _do.getDeviceType() != null ? _do.getDeviceType().getCode() : null;
        this.deviceCode = _do.getDeviceCode();
        this.capacity = _do.getCapacity();
        this.capacityUnit = _do.getCapacityUnit() != null ? _do.getCapacityUnit().getChineseName() : null;
        this.status = _do.getStatus() != null ? _do.getStatus().getCode() : null;
        return this;
    }
}
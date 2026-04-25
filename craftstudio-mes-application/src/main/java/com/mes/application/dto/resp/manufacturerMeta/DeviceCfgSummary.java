package com.mes.application.dto.resp.manufacturerMeta;

import com.mes.domain.manufacturer.device.entity.DeviceProcedure;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import lombok.Data;

import java.util.List;

@Data
public class DeviceCfgSummary {

    private String id;
    private String manufacturerMetaId;      // 所属制造商 ID
    private String deviceInfoId;                // 设备 ID
    private String deviceName;              // 设备名称
    private String deviceType;              // 设备类型 code
    private String deviceTypeName;          // 设备类型名称
    private String deviceCode;              // 设备编号
    private Double capacity;                // 产能
    private String capacityUnit;            // 产能单位 code
    private String capacityUnitName;        // 产能单位名称
    private String status;
    private boolean bound;
    private String boundVersion;
    private String brand;
    private List<DeviceProcedure> deviceProcedures;

    /**
     * 从 ManufacturerDeviceCfg 实体转换为响应 DTO
     * @param deviceCfg 设备配置实体
     * @return DeviceCfgSummary
     */
    public static DeviceCfgSummary from(ManufacturerDeviceCfg deviceCfg) {
        if (deviceCfg == null) {
            return null;
        }
        
        DeviceCfgSummary response = new DeviceCfgSummary();
        response.setId(deviceCfg.getId());
        response.setManufacturerMetaId(deviceCfg.getManufacturerMetaId());
        response.setDeviceInfoId(deviceCfg.getDeviceInfoId());
        response.setDeviceName(deviceCfg.getDeviceName());
        response.setDeviceType(deviceCfg.getDeviceType() != null ? deviceCfg.getDeviceType().getCode() : null);
        response.setDeviceTypeName(deviceCfg.getDeviceType() != null ? deviceCfg.getDeviceType().getChineseName() : null);
        response.setDeviceCode(deviceCfg.getDeviceCode());
        response.setCapacity(deviceCfg.getCapacity());
        response.setCapacityUnit(deviceCfg.getCapacityUnit() != null ? deviceCfg.getCapacityUnit().getSymbol() : null);
        response.setCapacityUnitName(deviceCfg.getCapacityUnit() != null ? deviceCfg.getCapacityUnit().getChineseName() : null);
        response.setStatus(deviceCfg.getStatus() != null ? deviceCfg.getStatus().getCode() : null);
        response.setBound(deviceCfg.isBound());
        response.setBoundVersion(deviceCfg.getBoundVersion());
        
        return response;
    }
}

package com.mes.application.dto.req.manufacturerMeta;

import com.mes.domain.manufacturer.device.enums.DeviceType;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerProcedureMeta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ManufacturerProcedure {
    
    @NotBlank(message = "工序 ID 不能为空")
    @Size(max = 50, message = "工序 ID 长度不能超过 50 个字符")
    private String procedureId;

    @NotBlank(message = "工序名称不能为空")
    @Size(max = 100, message = "工序名称长度不能超过 100 个字符")
    private String procedureName;

    @Size(max = 50, message = "设备 ID 长度不能超过 50 个字符")
    private String deviceId;

    @Size(max = 50, message = "设备类型长度不能超过 50 个字符")
    private String deviceType;

    /**
     * 转换为领域实体
     * @return ManufacturerProcedureMeta 领域实体
     */
    public ManufacturerProcedureMeta toDomainEntity() {
        ManufacturerProcedureMeta procedureMeta = new ManufacturerProcedureMeta();
        procedureMeta.setProcedureId(this.procedureId);
        procedureMeta.setProcedureName(this.procedureName);
        procedureMeta.setDeviceId(this.deviceId);
        procedureMeta.setDeviceType(DeviceType.getByCode(this.deviceType));
        return procedureMeta;
    }

    public boolean isValid() {
        if (procedureId == null || procedureId.trim().isEmpty()) {
            return false;
        }
        if (procedureName == null || procedureName.trim().isEmpty()) {
            return false;
        }
        if (procedureId.length() > 50) {
            return false;
        }
        if (procedureName.length() > 100) {
            return false;
        }
        if (deviceId != null && deviceId.length() > 50) {
            return false;
        }
        if (deviceType != null && deviceType.length() > 50) {
            return false;
        }
        return true;
    }

    public String getValidationMessage() {
        if (procedureId == null || procedureId.trim().isEmpty()) {
            return "工序 ID 不能为空";
        }
        if (procedureName == null || procedureName.trim().isEmpty()) {
            return "工序名称不能为空";
        }
        if (procedureId.length() > 50) {
            return "工序 ID 长度不能超过 50 个字符";
        }
        if (procedureName.length() > 100) {
            return "工序名称长度不能超过 100 个字符";
        }
        if (deviceId != null && deviceId.length() > 50) {
            return "设备 ID 长度不能超过 50 个字符";
        }
        if (deviceType != null && deviceType.length() > 50) {
            return "设备类型长度不能超过 50 个字符";
        }
        return "";
    }

    // Getters and Setters
    public String getProcedureId() {
        return procedureId;
    }

    public void setProcedureId(String procedureId) {
        this.procedureId = procedureId;
    }

    public String getProcedureName() {
        return procedureName;
    }

    public void setProcedureName(String procedureName) {
        this.procedureName = procedureName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }
}

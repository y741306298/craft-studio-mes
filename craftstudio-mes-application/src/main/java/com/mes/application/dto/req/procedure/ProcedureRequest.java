package com.mes.application.dto.req.procedure;

import com.mes.application.dto.req.base.ApiRequest;
import com.mes.domain.manufacturer.device.enums.DeviceType;
import com.mes.domain.manufacturer.procedure.entity.Procedure;
import com.mes.domain.manufacturer.procedure.entity.ProcedureAssetHandSign;
import com.mes.domain.manufacturer.procedure.entity.ProcedureAssetSign;
import com.mes.domain.manufacturer.procedure.entity.ProcedureInputSign;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProcedureRequest extends ApiRequest {
    private String id;

    private String procedureId;

    @NotBlank(message = "工序名称不能为空")
    @Size(max = 100, message = "工序名称长度不能超过 100 个字符")
    private String procedureName;

    @Size(max = 50, message = "工序类型长度不能超过 50 个字符")
    private String procedureType;

    @Size(max = 50, message = "设备类型长度不能超过 50 个字符")
    private String deviceType;

    @Size(max = 20, message = "状态长度不能超过 20 个字符")
    private String status;

    @Size(max = 200, message = "脚本 URL 长度不能超过 200 个字符")
    private String scriptUrl;

    private String remarks;

    @Valid
    private ProcedureAssetHandSign procedureAssetHandSigns; // 文件处理型

    @Valid
    private ProcedureInputSign procedureInputSigns;         // 参数交互性

    @Valid
    private ProcedureAssetSign rocedureAssetSign;           // 资源查看型

    /**
     * 转换为领域实体
     * @return Procedure 领域实体
     */
    public Procedure toDomainEntity() {
        Procedure procedure = new Procedure();

        // 基础字段映射
        procedure.setProcedureId(this.procedureId);
        procedure.setProcedureName(this.procedureName);
        procedure.setProcedureType(this.procedureType);
        procedure.setDeviceType(DeviceType.getByCode(this.deviceType));
        procedure.setStatus(this.status);
        procedure.setScriptUrl(this.scriptUrl);
        procedure.setRemarks(this.remarks);

        // 关联字段映射
        procedure.setProcedureAssetHandSigns(this.procedureAssetHandSigns);
        procedure.setProcedureInputSigns(this.procedureInputSigns);
        procedure.setRocedureAssetSign(this.rocedureAssetSign);

        return procedure;
    }

    @Override
    public boolean isValid() {
        // 基本非空验证
        if (procedureName == null || procedureName.trim().isEmpty()) {
            return false;
        }

        // 长度验证
        if (procedureName.length() > 100) {
            return false;
        }
        if (procedureType != null && procedureType.length() > 50) {
            return false;
        }
        if (deviceType != null && deviceType.length() > 50) {
            return false;
        }
        if (status != null && status.length() > 20) {
            return false;
        }
        if (scriptUrl != null && scriptUrl.length() > 200) {
            return false;
        }

        // 嵌套对象验证
        if (procedureAssetHandSigns != null && !isProcedureAssetHandSignValid(procedureAssetHandSigns)) {
            return false;
        }
        if (procedureInputSigns != null && !isProcedureInputSignValid(procedureInputSigns)) {
            return false;
        }
        if (rocedureAssetSign != null && !isProcedureAssetSignValid(rocedureAssetSign)) {
            return false;
        }

        return true;
    }

    @Override
    public String getValidationMessage() {
        if (procedureName == null || procedureName.trim().isEmpty()) {
            return "工序名称不能为空";
        }
        if (procedureName.length() > 100) {
            return "工序名称长度不能超过100个字符";
        }
        if (procedureType != null && procedureType.length() > 50) {
            return "工序类型长度不能超过50个字符";
        }
        if (deviceType != null && deviceType.length() > 50) {
            return "设备类型长度不能超过50个字符";
        }
        if (status != null && status.length() > 20) {
            return "状态长度不能超过20个字符";
        }
        if (scriptUrl != null && scriptUrl.length() > 200) {
            return "脚本URL长度不能超过200个字符";
        }

        // 嵌套对象验证消息
        if (procedureAssetHandSigns != null) {
            String assetHandSignMessage = getProcedureAssetHandSignValidationMessage(procedureAssetHandSigns);
            if (!assetHandSignMessage.isEmpty()) {
                return "文件处理型配置: " + assetHandSignMessage;
            }
        }

        if (procedureInputSigns != null) {
            String inputSignMessage = getProcedureInputSignValidationMessage(procedureInputSigns);
            if (!inputSignMessage.isEmpty()) {
                return "参数交互性配置: " + inputSignMessage;
            }
        }

        if (rocedureAssetSign != null) {
            String assetSignMessage = getProcedureAssetSignValidationMessage(rocedureAssetSign);
            if (!assetSignMessage.isEmpty()) {
                return "资源查看型配置: " + assetSignMessage;
            }
        }

        return "";
    }

    // 辅助验证方法
    private boolean isProcedureAssetHandSignValid(ProcedureAssetHandSign sign) {
        // 这里可以添加具体的验证逻辑
        return true;
    }

    private boolean isProcedureInputSignValid(ProcedureInputSign sign) {
        // 这里可以添加具体的验证逻辑
        return true;
    }

    private boolean isProcedureAssetSignValid(ProcedureAssetSign sign) {
        // 这里可以添加具体的验证逻辑
        return true;
    }

    private String getProcedureAssetHandSignValidationMessage(ProcedureAssetHandSign sign) {
        // 这里可以添加具体的验证消息
        return "";
    }

    private String getProcedureInputSignValidationMessage(ProcedureInputSign sign) {
        // 这里可以添加具体的验证消息
        return "";
    }

    private String getProcedureAssetSignValidationMessage(ProcedureAssetSign sign) {
        // 这里可以添加具体的验证消息
        return "";
    }
}

package com.mes.interfaces.api.dto.resp.procedure;

import com.mes.domain.manufacturer.device.enums.DeviceType;
import com.mes.domain.manufacturer.procedure.entity.Procedure;
import com.mes.domain.manufacturer.procedure.entity.ProcedureAssetHandSign;
import com.mes.domain.manufacturer.procedure.entity.ProcedureInputSign;
import com.mes.domain.manufacturer.procedure.entity.ProcedureAssetSign;
import lombok.Data;

import java.util.Date;

@Data
public class ProcedureListResponse {

    // 基础信息
    private String id;
    private String procedureId;
    private String procedureName;
    private String procedureType;
    private String deviceType;
    private String deviceTypeName;
    private String status;
    private String scriptUrl;
    private String remarks;
    
    // 时间信息
    private Date createTime;
    private Date updateTime;
    
    // 关联信息
    private ProcedureAssetHandSign procedureAssetHandSigns; // 文件处理型
    private ProcedureInputSign procedureInputSigns;         // 参数交互性
    private ProcedureAssetSign rocedureAssetSign;           // 资源查看型

    /**
     * 从 Procedure 实体转换为响应 DTO
     * @param procedure 工序实体
     * @return ProcedureListResponse
     */
    public static ProcedureListResponse from(Procedure procedure) {
        if (procedure == null) {
            return null;
        }
        
        ProcedureListResponse response = new ProcedureListResponse();
        
        // 基础字段映射
        response.setId(procedure.getId());
        response.setProcedureId(procedure.getProcedureId());
        response.setProcedureName(procedure.getProcedureName());
        response.setProcedureType(procedure.getProcedureType());
        response.setDeviceType(procedure.getDeviceType() != null ? procedure.getDeviceType().getCode() : null);
        response.setDeviceTypeName(procedure.getDeviceType() != null ? procedure.getDeviceType().getChineseName() : null);
        response.setStatus(procedure.getStatus());
        response.setScriptUrl(procedure.getScriptUrl());
        response.setRemarks(procedure.getRemarks());
        response.setCreateTime(procedure.getCreateTime());
        response.setUpdateTime(procedure.getUpdateTime());
        
        // 关联字段映射
        response.setProcedureAssetHandSigns(procedure.getProcedureAssetHandSigns());
        response.setProcedureInputSigns(procedure.getProcedureInputSigns());
        response.setRocedureAssetSign(procedure.getRocedureAssetSign());
        
        return response;
    }
}

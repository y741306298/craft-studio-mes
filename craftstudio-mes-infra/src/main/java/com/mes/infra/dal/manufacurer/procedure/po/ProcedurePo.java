package com.mes.infra.dal.manufacurer.procedure.po;

import com.mes.domain.manufacturer.device.enums.DeviceType;
import com.mes.domain.manufacturer.procedure.entity.Procedure;
import com.mes.domain.manufacturer.procedure.entity.ProcedureAssetHandSign;
import com.mes.domain.manufacturer.procedure.entity.ProcedureInputSign;
import com.mes.domain.manufacturer.procedure.entity.ProcedureAssetSign;
import com.mes.infra.base.BasePO;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "procedure")
public class ProcedurePo extends BasePO<Procedure> {

    private String procedureId;
    private String procedureName;
    private String procedureType;
    private String deviceType;
    private String status;
    private String scriptUrl;
    private ProcedureAssetHandSign procedureAssetHandSigns;
    private ProcedureInputSign procedureInputSigns;
    private ProcedureAssetSign rocedureAssetSign;
    private String remarks;

    @Override
    public Procedure toDO() {
        Procedure procedure = new Procedure();
        procedure.setId(getId());
        procedure.setCreateTime(getCreateTime());
        procedure.setUpdateTime(getUpdateTime());
        procedure.setProcedureId(procedureId);
        procedure.setProcedureName(procedureName);
        procedure.setProcedureType(procedureType);
        procedure.setDeviceType(DeviceType.getByCode(deviceType));
        procedure.setStatus(status);
        procedure.setScriptUrl(scriptUrl);
        procedure.setProcedureAssetHandSigns(procedureAssetHandSigns);
        procedure.setProcedureInputSigns(procedureInputSigns);
        procedure.setRocedureAssetSign(rocedureAssetSign);
        procedure.setRemarks(remarks);
        return procedure;
    }

    @Override
    protected BasePO<Procedure> fromDO(Procedure _do) {
        if (_do == null) {
            return null;
        }
        // 设置业务字段
        this.procedureId = _do.getProcedureId();
        this.procedureName = _do.getProcedureName();
        this.procedureType = _do.getProcedureType();
        this.deviceType = _do.getDeviceType() != null ? _do.getDeviceType().getCode() : null;
        this.status = _do.getStatus();
        this.scriptUrl = _do.getScriptUrl();
        this.procedureAssetHandSigns = _do.getProcedureAssetHandSigns();
        this.procedureInputSigns = _do.getProcedureInputSigns();
        this.rocedureAssetSign = _do.getRocedureAssetSign();
        this.remarks = _do.getRemarks();
        return this;
    }
}

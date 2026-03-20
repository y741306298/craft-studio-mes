package com.mes.domain.manufacturer.procedure.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.device.enums.DeviceType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Procedure extends BaseEntity {

    private String procedureId;
    private String procedureName;
    private String procedureType;
    private DeviceType deviceType;
    private String status;
    private String scriptUrl;
    private ProcedureAssetHandSign procedureAssetHandSigns;//文件处理型
    private ProcedureInputSign procedureInputSigns;//参数交互性
    private ProcedureAssetSign rocedureAssetSign;//资源查看型
    private String remarks;

}

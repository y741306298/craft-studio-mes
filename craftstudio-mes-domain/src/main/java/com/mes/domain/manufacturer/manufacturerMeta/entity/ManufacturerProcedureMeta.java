package com.mes.domain.manufacturer.manufacturerMeta.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.device.enums.DeviceType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ManufacturerProcedureMeta extends BaseEntity {

    private String procedureId;

    private String procedureName;

    private String deviceId;

    private DeviceType deviceType;

}

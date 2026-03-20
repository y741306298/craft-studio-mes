package com.mes.domain.manufacturer.device.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceProcedure extends BaseEntity {
    private String procedureId;
    private String procedureName;
}

package com.mes.domain.manufacturer.device.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeviceMaterial extends BaseEntity {
    private String materialId;
    private String materialName;
}

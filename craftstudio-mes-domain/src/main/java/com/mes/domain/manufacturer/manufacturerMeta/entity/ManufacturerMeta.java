package com.mes.domain.manufacturer.manufacturerMeta.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.manufacturerMeta.enums.ManufacturerType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ManufacturerMeta extends BaseEntity {
    private String manufacturerMetaId;
    private ManufacturerType manufacturerMetaType;
    private String name;
    private String description;
    private List<ManufacturerWorkshopMeta> manufacturerWorkshopMetas;

}

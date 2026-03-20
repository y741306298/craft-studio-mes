package com.mes.domain.manufacturer.manufacturerMeta.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ManufacturerWorkshopMeta extends BaseEntity {

    private String workshopId;
    private String workshopName;
    private String status;
    private List<ManufacturerProductionLineMeta> manufacturerProductionLineMetas;

}

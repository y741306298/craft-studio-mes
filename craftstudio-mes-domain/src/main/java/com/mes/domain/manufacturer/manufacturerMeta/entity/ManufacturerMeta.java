package com.mes.domain.manufacturer.manufacturerMeta.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.enums.CfgStatus;
import com.mes.domain.manufacturer.manufacturerMeta.enums.ManufacturerType;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Consignee;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ManufacturerMeta extends BaseEntity {
    private String manufacturerMetaId;
    private String manufacturerTempId;
    private ManufacturerType manufacturerMetaType;
    private Consignee consignee;
    private Address address;
    private String name;
    private String description;
    private CfgStatus status;
    private List<ManufacturerWorkshopMeta> manufacturerWorkshopMetas;

}

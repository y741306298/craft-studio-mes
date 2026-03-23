package com.mes.infra.dal.manufacurer.manufacturerMeta.po;

import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerMeta;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerWorkshopMeta;
import com.mes.domain.manufacturer.manufacturerMeta.enums.ManufacturerType;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "manufacturerMeta")
public class ManufacturerMetaPo extends BasePO<ManufacturerMeta> {

    private String manufacturerMetaId;
    private String manufacturerMetaType;
    private String name;
    private String description;
    private List<ManufacturerWorkshopMeta> manufacturerWorkshopMetas;

    @Override
    public ManufacturerMeta toDO() {
        ManufacturerMeta manufacturerMeta = new ManufacturerMeta();
        manufacturerMeta.setId(getId());
        copyBaseFieldsToDO(manufacturerMeta);
        manufacturerMeta.setManufacturerMetaId(manufacturerMetaId);
        manufacturerMeta.setManufacturerMetaType(ManufacturerType.getByCode(manufacturerMetaType));
        manufacturerMeta.setName(name);
        manufacturerMeta.setDescription(description);
        manufacturerMeta.setManufacturerWorkshopMetas(manufacturerWorkshopMetas);
        return manufacturerMeta;
    }

    @Override
    protected BasePO<ManufacturerMeta> fromDO(ManufacturerMeta _do) {
        if (_do == null) {
            return null;
        }
        // 设置业务字段
        this.manufacturerMetaId = _do.getManufacturerMetaId();
        this.manufacturerMetaType = _do.getManufacturerMetaType() != null ? _do.getManufacturerMetaType().getCode() : null;
        this.name = _do.getName();
        this.description = _do.getDescription();
        this.manufacturerWorkshopMetas = _do.getManufacturerWorkshopMetas();
        return this;
    }
}
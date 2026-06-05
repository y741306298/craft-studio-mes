package com.mes.infra.dal.manufacurer.typesetting.po;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingContainerWidthInset;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "typesetting_container_width_inset")
public class TypesettingContainerWidthInsetPo extends BasePO<TypesettingContainerWidthInset> {

    private String materialId;
    private String layoutMode;
    private Integer widthInset;

    @Override
    public TypesettingContainerWidthInset toDO() {
        TypesettingContainerWidthInset inset = new TypesettingContainerWidthInset();
        inset.setId(getId());
        inset.setCreateTime(getCreateTime());
        inset.setUpdateTime(getUpdateTime());
        inset.setMaterialId(materialId);
        inset.setLayoutMode(layoutMode);
        inset.setWidthInset(widthInset);
        return inset;
    }

    @Override
    protected BasePO<TypesettingContainerWidthInset> fromDO(TypesettingContainerWidthInset _do) {
        this.materialId = _do.getMaterialId();
        this.layoutMode = _do.getLayoutMode();
        this.widthInset = _do.getWidthInset();
        return this;
    }
}

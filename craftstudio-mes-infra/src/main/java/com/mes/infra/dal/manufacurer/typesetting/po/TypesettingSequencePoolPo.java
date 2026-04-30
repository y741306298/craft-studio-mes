package com.mes.infra.dal.manufacurer.typesetting.po;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingSequencePool;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "typesetting_sequence_pool")
public class TypesettingSequencePoolPo extends BasePO<TypesettingSequencePool> {

    private String manufacturerMetaId;
    private String usageType;
    private List<Integer> sequenceArray;

    @Override
    public TypesettingSequencePool toDO() {
        TypesettingSequencePool pool = new TypesettingSequencePool();
        pool.setId(getId());
        pool.setCreateTime(getCreateTime());
        pool.setUpdateTime(getUpdateTime());
        pool.setManufacturerMetaId(manufacturerMetaId);
        pool.setUsageType(usageType);
        pool.setSequenceArray(sequenceArray);
        return pool;
    }

    @Override
    protected BasePO<TypesettingSequencePool> fromDO(TypesettingSequencePool _do) {
        this.manufacturerMetaId = _do.getManufacturerMetaId();
        this.usageType = _do.getUsageType();
        this.sequenceArray = _do.getSequenceArray();
        return this;
    }
}

package com.mes.infra.dal.manufacurer.typesetting;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingSequencePool;
import com.mes.domain.manufacturer.typesetting.repository.TypesettingSequencePoolRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.typesetting.po.TypesettingSequencePoolPo;
import org.springframework.stereotype.Repository;

@Repository
public class TypesettingSequencePoolRepositoryImp extends BaseRepositoryImp<TypesettingSequencePool, TypesettingSequencePoolPo>
        implements TypesettingSequencePoolRepository {

    @Override
    public Class<TypesettingSequencePoolPo> poClass() {
        return TypesettingSequencePoolPo.class;
    }
}

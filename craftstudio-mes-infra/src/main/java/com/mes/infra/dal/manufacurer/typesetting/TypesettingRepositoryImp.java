package com.mes.infra.dal.manufacurer.typesetting;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.repository.TypesettingRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.typesetting.po.TypesettingPo;
import org.springframework.stereotype.Repository;

@Repository
public class TypesettingRepositoryImp extends BaseRepositoryImp<TypesettingInfo, TypesettingPo> implements TypesettingRepository {

    @Override
    public Class<TypesettingPo> poClass() {
        return TypesettingPo.class;
    }
}

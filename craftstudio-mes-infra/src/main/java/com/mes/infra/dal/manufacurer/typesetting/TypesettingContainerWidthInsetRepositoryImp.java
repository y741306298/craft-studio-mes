package com.mes.infra.dal.manufacurer.typesetting;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingContainerWidthInset;
import com.mes.domain.manufacturer.typesetting.repository.TypesettingContainerWidthInsetRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.typesetting.po.TypesettingContainerWidthInsetPo;
import org.springframework.stereotype.Repository;

@Repository
public class TypesettingContainerWidthInsetRepositoryImp
        extends BaseRepositoryImp<TypesettingContainerWidthInset, TypesettingContainerWidthInsetPo>
        implements TypesettingContainerWidthInsetRepository {

    @Override
    public Class<TypesettingContainerWidthInsetPo> poClass() {
        return TypesettingContainerWidthInsetPo.class;
    }
}

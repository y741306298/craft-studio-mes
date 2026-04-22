package com.mes.infra.dal.manufacurer.typesetting;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingPrintTask;
import com.mes.domain.manufacturer.typesetting.repository.TypesettingPrintTaskRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.typesetting.po.TypesettingPrintTaskPo;
import org.springframework.stereotype.Repository;

@Repository
public class TypesettingPrintTaskRepositoryImp extends BaseRepositoryImp<TypesettingPrintTask, TypesettingPrintTaskPo>
        implements TypesettingPrintTaskRepository {

    @Override
    public Class<TypesettingPrintTaskPo> poClass() {
        return TypesettingPrintTaskPo.class;
    }
}

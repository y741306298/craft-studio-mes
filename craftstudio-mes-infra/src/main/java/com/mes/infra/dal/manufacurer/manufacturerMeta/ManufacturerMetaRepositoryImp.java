package com.mes.infra.dal.manufacurer.manufacturerMeta;

import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerMeta;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerMetaRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.manufacturerMeta.po.ManufacturerMetaPo;
import org.springframework.stereotype.Repository;

@Repository
public class ManufacturerMetaRepositoryImp extends BaseRepositoryImp<ManufacturerMeta, ManufacturerMetaPo> implements ManufacturerMetaRepository {


    @Override
    public Class<ManufacturerMetaPo> poClass() {
        return ManufacturerMetaPo.class;
    }
}

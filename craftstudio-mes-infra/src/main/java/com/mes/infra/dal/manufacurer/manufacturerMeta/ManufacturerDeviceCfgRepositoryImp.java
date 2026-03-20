package com.mes.infra.dal.manufacurer.manufacturerMeta;

import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerDeviceCfgRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.manufacturerMeta.po.ManufacturerDeviceCfgPo;
import org.springframework.stereotype.Repository;

@Repository
public class ManufacturerDeviceCfgRepositoryImp extends BaseRepositoryImp<ManufacturerDeviceCfg, ManufacturerDeviceCfgPo> implements ManufacturerDeviceCfgRepository {

    @Override
    public Class<ManufacturerDeviceCfgPo> poClass() {
        return ManufacturerDeviceCfgPo.class;
    }
}

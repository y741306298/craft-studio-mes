package com.mes.infra.dal.manufacurer.manufacturerMaterialLayoutSpecCfg;

import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.entity.ManufacturerMaterialLayoutSpecCfg;
import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.repository.ManufacturerMaterialLayoutSpecCfgRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.manufacturerMaterialLayoutSpecCfg.po.ManufacturerMaterialLayoutSpecCfgPo;
import org.springframework.stereotype.Repository;

@Repository
public class ManufacturerMaterialLayoutSpecCfgRepositoryImp
        extends BaseRepositoryImp<ManufacturerMaterialLayoutSpecCfg, ManufacturerMaterialLayoutSpecCfgPo>
        implements ManufacturerMaterialLayoutSpecCfgRepository {

    @Override
    public Class<ManufacturerMaterialLayoutSpecCfgPo> poClass() {
        return ManufacturerMaterialLayoutSpecCfgPo.class;
    }
}

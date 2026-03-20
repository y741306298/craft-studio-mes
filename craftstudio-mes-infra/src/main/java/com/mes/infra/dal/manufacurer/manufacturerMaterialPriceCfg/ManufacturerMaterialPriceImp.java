package com.mes.infra.dal.manufacurer.manufacturerMaterialPriceCfg;

import com.mes.domain.manufacturer.manufacturerMaterialPriceCfg.entity.ManufacturerMaterialPriceCfg;
import com.mes.domain.manufacturer.manufacturerMaterialPriceCfg.repository.ManufacturerMaterialPriceRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.manufacturerMaterialPriceCfg.po.ManufacturerMaterialPricePo;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ManufacturerMaterialPriceImp extends BaseRepositoryImp<ManufacturerMaterialPriceCfg, ManufacturerMaterialPricePo> implements ManufacturerMaterialPriceRepository {


    @Override
    public Class<ManufacturerMaterialPricePo> poClass() {
        return null;
    }
}

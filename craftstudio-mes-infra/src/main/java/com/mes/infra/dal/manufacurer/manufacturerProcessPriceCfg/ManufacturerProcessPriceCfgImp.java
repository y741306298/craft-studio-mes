package com.mes.infra.dal.manufacurer.manufacturerProcessPriceCfg;

import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.entity.ManufacturerProcessPriceCfg;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.repository.ManufacturerProcessPriceCfgRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.manufacturerProcessPriceCfg.po.ManufacturerProcessPriceCfgPo;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ManufacturerProcessPriceCfgImp extends BaseRepositoryImp<ManufacturerProcessPriceCfg, ManufacturerProcessPriceCfgPo> implements ManufacturerProcessPriceCfgRepository {

    @Override
    public Class<ManufacturerProcessPriceCfgPo> poClass() {
        return ManufacturerProcessPriceCfgPo.class;
    }

    public List<ManufacturerProcessPriceCfg> findByManufacturerId(String manufacturerId, long current, int size) {
        return filterList(current, size, java.util.Map.of("manufacturerId", manufacturerId));
    }

    public long countByManufacturerId(String manufacturerId) {
        return filterTotal(java.util.Map.of("manufacturerId", manufacturerId));
    }
}

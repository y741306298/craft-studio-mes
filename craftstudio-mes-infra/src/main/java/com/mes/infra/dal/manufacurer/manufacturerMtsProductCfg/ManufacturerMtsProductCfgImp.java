package com.mes.infra.dal.manufacurer.manufacturerMtsProductCfg;

import com.mes.domain.manufacturer.manufacturerMtsProductCfg.entity.ManufacturerMtsProductCfg;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.repository.ManufacturerMtsProductCfgRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.manufacturerMtsProductCfg.po.ManufacturerMtsProductCfgPo;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ManufacturerMtsProductCfgImp extends BaseRepositoryImp<ManufacturerMtsProductCfg, ManufacturerMtsProductCfgPo> implements ManufacturerMtsProductCfgRepository {

    @Override
    public Class<ManufacturerMtsProductCfgPo> poClass() {
        return ManufacturerMtsProductCfgPo.class;
    }

    public List<ManufacturerMtsProductCfg> findByManufacturerId(String manufacturerId, long current, int size) {
        return filterList(current, size, java.util.Map.of("manufacturerId", manufacturerId));
    }

    public long countByManufacturerId(String manufacturerId) {
        return filterTotal(java.util.Map.of("manufacturerId", manufacturerId));
    }
    
    public List<ManufacturerMtsProductCfg> findByManufacturerIdAndProductId(String manufacturerId, String productId, long current, int size) {
        return filterList(current, size, java.util.Map.of(
            "manufacturerId", manufacturerId,
            "productId", productId
        ));
    }
    
    public long countByManufacturerIdAndProductId(String manufacturerId, String productId) {
        return filterTotal(java.util.Map.of(
            "manufacturerId", manufacturerId,
            "productId", productId
        ));
    }
}

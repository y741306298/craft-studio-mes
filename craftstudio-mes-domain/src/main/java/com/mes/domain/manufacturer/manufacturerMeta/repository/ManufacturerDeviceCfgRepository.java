package com.mes.domain.manufacturer.manufacturerMeta.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;

import java.util.Collection;
import java.util.Map;

public interface ManufacturerDeviceCfgRepository extends BaseRepository<ManufacturerDeviceCfg> {
    Map<String, Long> countByManufacturerMetaIds(Collection<String> manufacturerMetaIds);
}

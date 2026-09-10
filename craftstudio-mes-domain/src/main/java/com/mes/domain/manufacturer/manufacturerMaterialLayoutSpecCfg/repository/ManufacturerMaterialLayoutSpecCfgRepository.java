package com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.entity.ManufacturerMaterialLayoutSpecCfg;

public interface ManufacturerMaterialLayoutSpecCfgRepository extends BaseRepository<ManufacturerMaterialLayoutSpecCfg> {

    /**
     * 将历史阶梯数据中以厘米存储的 insetCm 转换为毫米字段 insetMm。
     *
     * @return 实际修改的 MongoDB 文档数
     */
    long migrateInsetCmToMm();
}

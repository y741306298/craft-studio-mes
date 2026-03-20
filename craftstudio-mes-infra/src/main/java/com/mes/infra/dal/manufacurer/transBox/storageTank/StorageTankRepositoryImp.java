package com.mes.infra.dal.manufacurer.transBox.storageTank;

import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageTank;
import com.mes.domain.manufacturer.transBox.storageTank.repository.StorageTankRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.transBox.storageTank.po.StorageTankPo;
import org.springframework.stereotype.Repository;

@Repository
public class StorageTankRepositoryImp extends BaseRepositoryImp<StorageTank, StorageTankPo> implements StorageTankRepository {

    @Override
    public Class<StorageTankPo> poClass() {
        return StorageTankPo.class;
    }
}

package com.mes.infra.dal.manufacurer.transBox.storageSlot;

import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageSlot;
import com.mes.domain.manufacturer.transBox.storageTank.repository.StorageSlotRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.transBox.storageSlot.po.StorageSlotPo;
import org.springframework.stereotype.Repository;

@Repository
public class StorageSlotRepositoryImp extends BaseRepositoryImp<StorageSlot, StorageSlotPo> implements StorageSlotRepository {

    @Override
    public Class<StorageSlotPo> poClass() {
        return StorageSlotPo.class;
    }
}

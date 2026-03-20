package com.mes.infra.dal.manufacurer.transBox.storageInventory;

import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageInventory;
import com.mes.domain.manufacturer.transBox.storageTank.repository.StorageInventoryRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.transBox.storageInventory.po.StorageInventoryPo;
import org.springframework.stereotype.Repository;

@Repository
public class StorageInventoryRepositoryImp extends BaseRepositoryImp<StorageInventory, StorageInventoryPo> implements StorageInventoryRepository {

    @Override
    public Class<StorageInventoryPo> poClass() {
        return StorageInventoryPo.class;
    }
}

package com.mes.infra.dal.manufacurer.transBox.storageOperationRecord;

import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageOperationRecord;
import com.mes.domain.manufacturer.transBox.storageTank.repository.StorageOperationRecordRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.transBox.storageOperationRecord.po.StorageOperationRecordPo;
import org.springframework.stereotype.Repository;

@Repository
public class StorageOperationRecordRepositoryImp extends BaseRepositoryImp<StorageOperationRecord, StorageOperationRecordPo> implements StorageOperationRecordRepository {

    @Override
    public Class<StorageOperationRecordPo> poClass() {
        return StorageOperationRecordPo.class;
    }
}

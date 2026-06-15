package com.mes.infra.dal.shared.algorithm;

import com.mes.domain.shared.algorithm.entity.AlgorithmCoreApiCallRecord;
import com.mes.domain.shared.algorithm.repository.AlgorithmCoreApiCallRecordRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.shared.algorithm.po.AlgorithmCoreApiCallRecordPo;
import org.springframework.stereotype.Repository;

@Repository
public class AlgorithmCoreApiCallRecordRepositoryImp extends BaseRepositoryImp<AlgorithmCoreApiCallRecord, AlgorithmCoreApiCallRecordPo> implements AlgorithmCoreApiCallRecordRepository {

    @Override
    public Class<AlgorithmCoreApiCallRecordPo> poClass() {
        return AlgorithmCoreApiCallRecordPo.class;
    }
}

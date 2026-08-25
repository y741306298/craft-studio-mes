package com.mes.infra.dal.shared.algorithm;

import com.mes.domain.shared.algorithm.entity.AlgorithmCoreApiCallRecord;
import com.mes.domain.shared.algorithm.repository.AlgorithmCoreApiCallRecordRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.shared.algorithm.po.AlgorithmCoreApiCallRecordPo;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

@Repository
public class AlgorithmCoreApiCallRecordRepositoryImp extends BaseRepositoryImp<AlgorithmCoreApiCallRecord, AlgorithmCoreApiCallRecordPo> implements AlgorithmCoreApiCallRecordRepository {

    @Override
    public Class<AlgorithmCoreApiCallRecordPo> poClass() {
        return AlgorithmCoreApiCallRecordPo.class;
    }

    @Override
    public AlgorithmCoreApiCallRecord findLatestByTypeAndSourceId(String type, String sourceId) {
        AlgorithmCoreApiCallRecordPo po = mongoTemplate.findOne(
                new SoftDeleteQuery(Criteria.where("type").is(type).and("sourceId").is(sourceId))
                        .with(Sort.by(Sort.Direction.DESC, "createTime")),
                poClass());
        return po == null ? null : po.toDO();
    }
}

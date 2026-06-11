package com.mes.infra.dal.delivery.deliveryRoute;

import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecord;
import com.mes.domain.delivery.deliveryRoute.repository.AddressRecognitionRecordRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.delivery.deliveryRoute.po.AddressRecognitionRecordPo;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AddressRecognitionRecordRepositoryImp extends BaseRepositoryImp<AddressRecognitionRecord, AddressRecognitionRecordPo> implements AddressRecognitionRecordRepository {

    @Override
    public Class<AddressRecognitionRecordPo> poClass() {
        return AddressRecognitionRecordPo.class;
    }

    @Override
    public AddressRecognitionRecord findByAddress(String terminalRegionCode, String detailAddress) {
        Query query = new SoftDeleteQuery(
                Criteria.where("address.terminalRegionCode").is(terminalRegionCode)
                        .and("address.detailAddress").is(detailAddress)
        );
        AddressRecognitionRecordPo po = mongoTemplate.findOne(query, poClass());
        return po == null ? null : po.toDO();
    }

    @Override
    public List<AddressRecognitionRecord> listByStatus(String status, long current, int size) {
        Query query = new SoftDeleteQuery(Criteria.where("status").is(status));
        query.with(Sort.by(Sort.Direction.DESC, "updateTime"));
        query.skip((current - 1) * size).limit(size);
        List<AddressRecognitionRecordPo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(AddressRecognitionRecordPo::toDO).toList();
    }

    @Override
    public long totalByStatus(String status) {
        Query query = new SoftDeleteQuery(Criteria.where("status").is(status));
        return mongoTemplate.count(query, poClass());
    }

}

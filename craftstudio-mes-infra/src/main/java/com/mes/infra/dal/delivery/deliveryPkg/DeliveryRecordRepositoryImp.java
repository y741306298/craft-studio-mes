package com.mes.infra.dal.delivery.deliveryPkg;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryRecord;
import com.mes.domain.delivery.deliveryPkg.repository.DeliveryRecordRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.delivery.deliveryPkg.po.DeliveryRecordPO;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class DeliveryRecordRepositoryImp extends BaseRepositoryImp<DeliveryRecord, DeliveryRecordPO> implements DeliveryRecordRepository {

    @Override
    public Class<DeliveryRecordPO> poClass() {
        return DeliveryRecordPO.class;
    }

    @Override
    public List<DeliveryRecord> findByOrderId(String orderId) {
        Query query = new Query(Criteria.where("orderId").is(orderId).and("deleteAt").is(null));
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        List<DeliveryRecordPO> pos = mongoTemplate.find(query, DeliveryRecordPO.class);
        return pos.stream().map(DeliveryRecordPO::toDO).collect(Collectors.toList());
    }

    @Override
    public DeliveryRecord findByTrackingNumber(String trackingNumber) {
        Query query = new Query(Criteria.where("trackingNumber").is(trackingNumber).and("deleteAt").is(null));
        DeliveryRecordPO po = mongoTemplate.findOne(query, DeliveryRecordPO.class);
        return po != null ? po.toDO() : null;
    }

    @Override
    public List<DeliveryRecord> findByManufacturerMetaId(String manufacturerMetaId, int current, int size) {
        Query query = new Query(Criteria.where("manufacturerMetaId").is(manufacturerMetaId).and("deleteAt").is(null));
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        query.skip((long) (current - 1) * size);
        query.limit(size);
        List<DeliveryRecordPO> pos = mongoTemplate.find(query, DeliveryRecordPO.class);
        return pos.stream().map(DeliveryRecordPO::toDO).collect(Collectors.toList());
    }
}

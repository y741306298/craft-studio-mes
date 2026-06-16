package com.mes.infra.dal.delivery.deliveryRoute;

import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecord;
import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecordStatus;
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
    public AddressRecognitionRecord findByAddress(String manufacturerMetaId, String terminalRegionCode, String detailAddress) {
        Query query = new SoftDeleteQuery(
                Criteria.where("manufacturerMetaId").is(manufacturerMetaId)
                        .and("address.terminalRegionCode").is(terminalRegionCode)
                        .and("address.detailAddress").is(detailAddress)
        );
        AddressRecognitionRecordPo po = mongoTemplate.findOne(query, poClass());
        return po == null ? null : po.toDO();
    }

    @Override
    public List<AddressRecognitionRecord> listByStatus(String status, String manufacturerMetaId, String detailAddress, long current, int size) {
        Query query = new SoftDeleteQuery(buildStatusCriteria(status, manufacturerMetaId, detailAddress));
        query.with(Sort.by(Sort.Direction.DESC, "updateTime"));
        query.skip((current - 1) * size).limit(size);
        List<AddressRecognitionRecordPo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(AddressRecognitionRecordPo::toDO).toList();
    }

    @Override
    public long totalByStatus(String status, String manufacturerMetaId, String detailAddress) {
        Query query = new SoftDeleteQuery(buildStatusCriteria(status, manufacturerMetaId, detailAddress));
        return mongoTemplate.count(query, poClass());
    }

    @Override
    public List<AddressRecognitionRecord> listAssignedByRouteNode(String routeId, String nodeId, String detailAddress, long current, int size) {
        Query query = new SoftDeleteQuery(buildAssignedRouteNodeCriteria(routeId, nodeId, detailAddress));
        query.with(Sort.by(Sort.Direction.DESC, "updateTime"));
        query.skip((current - 1) * size).limit(size);
        List<AddressRecognitionRecordPo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(AddressRecognitionRecordPo::toDO).toList();
    }

    @Override
    public long totalAssignedByRouteNode(String routeId, String nodeId, String detailAddress) {
        Query query = new SoftDeleteQuery(buildAssignedRouteNodeCriteria(routeId, nodeId, detailAddress));
        return mongoTemplate.count(query, poClass());
    }

    @Override
    public Integer findMaxOrderByRouteNode(String routeId, String nodeId) {
        Query query = new SoftDeleteQuery(
                Criteria.where("status").is(AddressRecognitionRecordStatus.ASSIGNED.getValue())
                        .and("routeId").is(routeId)
                        .and("nodeId").is(nodeId)
                        .and("order").ne(null)
        );
        query.with(Sort.by(Sort.Direction.DESC, "order"));
        query.limit(1);
        AddressRecognitionRecordPo po = mongoTemplate.findOne(query, poClass());
        return po == null ? null : po.getOrder();
    }

    private Criteria buildStatusCriteria(String status, String manufacturerMetaId, String detailAddress) {
        Criteria criteria = Criteria.where("status").is(status)
                .and("manufacturerMetaId").is(manufacturerMetaId);
        addDetailAddressCriteria(criteria, detailAddress);
        return criteria;
    }

    private Criteria buildAssignedRouteNodeCriteria(String routeId, String nodeId, String detailAddress) {
        Criteria criteria = Criteria.where("status").is(AddressRecognitionRecordStatus.ASSIGNED.getValue())
                .and("routeId").is(routeId)
                .and("nodeId").is(nodeId);
        addDetailAddressCriteria(criteria, detailAddress);
        return criteria;
    }

    private void addDetailAddressCriteria(Criteria criteria, String detailAddress) {
        if (detailAddress != null && !detailAddress.trim().isEmpty()) {
            criteria.and("address.detailAddress").regex(detailAddress, "i");
        }
    }

}

package com.mes.infra.dal.delivery.deliveryPkg;

import com.mes.domain.delivery.deliveryPkg.entity.DeliverySiid;
import com.mes.domain.delivery.deliveryPkg.repository.DeliverySiidRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.delivery.deliveryPkg.po.DeliverySiidPO;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class DeliverySiidRepositoryImp extends BaseRepositoryImp<DeliverySiid, DeliverySiidPO> implements DeliverySiidRepository {

    @Override
    public Class<DeliverySiidPO> poClass() {
        return DeliverySiidPO.class;
    }

    @Override
    public DeliverySiid findBySiid(String siid) {
        Query query = new Query(Criteria.where("siid").is(siid).and("deleteAt").is(null));
        DeliverySiidPO po = mongoTemplate.findOne(query, DeliverySiidPO.class);
        return po != null ? po.toDO() : null;
    }

    @Override
    public List<DeliverySiid> findByUserId(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId).and("deleteAt").is(null));
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        List<DeliverySiidPO> pos = mongoTemplate.find(query, DeliverySiidPO.class);
        return pos.stream().map(DeliverySiidPO::toDO).collect(Collectors.toList());
    }

    @Override
    public DeliverySiid findByDeliverySiidIdAndManufacturerMetaId(String deliverySiidId, String manufacturerMetaId) {
        Query query = new Query(Criteria.where("id").is(deliverySiidId)
                .and("manufacturerMetaId").is(manufacturerMetaId)
                .and("deleteAt").is(null));
        DeliverySiidPO po = mongoTemplate.findOne(query, DeliverySiidPO.class);
        return po != null ? po.toDO() : null;
    }
}

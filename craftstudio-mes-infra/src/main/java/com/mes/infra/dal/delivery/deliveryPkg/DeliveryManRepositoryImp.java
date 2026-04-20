package com.mes.infra.dal.delivery.deliveryPkg;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryMan;
import com.mes.domain.delivery.deliveryPkg.repository.DeliveryManRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.delivery.deliveryPkg.po.DeliveryManPO;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class DeliveryManRepositoryImp extends BaseRepositoryImp<DeliveryMan, DeliveryManPO> implements DeliveryManRepository {

    @Override
    public Class<DeliveryManPO> poClass() {
        return DeliveryManPO.class;
    }

    @Override
    public List<DeliveryMan> findByUserId(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId).and("deleteAt").is(null));
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        List<DeliveryManPO> pos = mongoTemplate.find(query, DeliveryManPO.class);
        return pos.stream().map(DeliveryManPO::toDO).collect(Collectors.toList());
    }

    @Override
    public DeliveryMan findByDeliveryManIdAndManufacturerMetaId(String deliveryManId, String manufacturerMetaId) {
        Query query = new Query(Criteria.where("id").is(deliveryManId)
                .and("manufacturerMetaId").is(manufacturerMetaId)
                .and("deleteAt").is(null));
        DeliveryManPO po = mongoTemplate.findOne(query, DeliveryManPO.class);
        return po != null ? po.toDO() : null;
    }
}

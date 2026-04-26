package com.mes.infra.dal.delivery.deliveryRoute;

import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNode;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteNodeRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.delivery.deliveryRoute.po.DeliveryRouteNodePo;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public class DeliveryRouteNodeRepositoryImp extends BaseRepositoryImp<DeliveryRouteNode, DeliveryRouteNodePo> implements DeliveryRouteNodeRepository {

    @Override
    public Class<DeliveryRouteNodePo> poClass() {
        return DeliveryRouteNodePo.class;
    }

    @Override
    public List<DeliveryRouteNode> listByRouteId(String routeId) {
        Query query = new SoftDeleteQuery(Criteria.where("routeId").is(routeId));
        query.with(Sort.by(Sort.Direction.ASC, "nodeOrder").and(Sort.by(Sort.Direction.ASC, "createTime")));
        List<DeliveryRouteNodePo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryRouteNodePo::toDO).toList();
    }

    @Override
    public void removeByRouteId(String routeId) {
        Query query = new Query(Criteria.where("routeId").is(routeId).and("deleteAt").is(null));
        Update update = new Update().set("deleteAt", new Date());
        mongoTemplate.updateMulti(query, update, poClass());
    }

    @Override
    public DeliveryRouteNode findByRouteNodeId(String routeNodeId) {
        Query query = new SoftDeleteQuery(Criteria.where("routeNodeId").is(routeNodeId));
        DeliveryRouteNodePo po = mongoTemplate.findOne(query, poClass());
        return po == null ? null : po.toDO();
    }
}

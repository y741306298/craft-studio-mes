package com.mes.infra.dal.delivery.deliveryRoute;

import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.delivery.deliveryRoute.po.DeliveryRoutePo;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class DeliveryRouteRepositoryImp extends BaseRepositoryImp<DeliveryRoute, DeliveryRoutePo> implements DeliveryRouteRepository {
    
    @Override
    public Class<DeliveryRoutePo> poClass() {
        return DeliveryRoutePo.class;
    }

    @Override
    public List<DeliveryRoute> listByManufacturerId(String manufacturerId, long current, int size) {
        Criteria criteria = Criteria.where("manufacturerMetaId").is(manufacturerId).and("deleteAt").is(null);
        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        query.skip((current - 1) * size).limit(size);
        List<DeliveryRoutePo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryRoutePo::toDO).collect(Collectors.toList());
    }

    @Override
    public long totalByManufacturerId(String manufacturerId) {
        Criteria criteria = Criteria.where("manufacturerMetaId").is(manufacturerId).and("deleteAt").is(null);
        Query query = new Query(criteria);
        return mongoTemplate.count(query, poClass());
    }
    
    /**
     * 根据路线名称查询
     */
    public List<DeliveryRoute> findByName(String routeName) {
        Criteria criteria = Criteria.where("routeName").is(routeName).and("deleteAt").is(null);
        Query query = new Query(criteria);
        List<DeliveryRoutePo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryRoutePo::toDO).toList();
    }
    
    /**
     * 根据状态查询配送路线
     */
    public List<DeliveryRoute> findByStatus(String status) {
        Criteria criteria = Criteria.where("status").is(status).and("deleteAt").is(null);
        Query query = new Query(criteria);
        List<DeliveryRoutePo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryRoutePo::toDO).toList();
    }
    
    /**
     * 根据路线类型查询
     */
    public List<DeliveryRoute> findByRouteType(String routeType) {
        Criteria criteria = Criteria.where("routeType").is(routeType).and("deleteAt").is(null);
        Query query = new Query(criteria);
        List<DeliveryRoutePo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryRoutePo::toDO).toList();
    }
    
    /**
     * 查询可用的配送路线
     */
    public List<DeliveryRoute> findAvailableRoutes() {
        Criteria criteria = Criteria.where("status").is("ACTIVE").and("deleteAt").is(null);
        Query query = new Query(criteria);
        List<DeliveryRoutePo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryRoutePo::toDO).toList();
    }
}

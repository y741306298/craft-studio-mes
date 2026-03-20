package com.mes.infra.dal.delivery.deliveryRoute;

import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.delivery.deliveryRoute.po.DeliveryRoutePo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DeliveryRouteRepositoryImp extends BaseRepositoryImp<DeliveryRoute, DeliveryRoutePo> implements DeliveryRouteRepository {
    
    @Override
    public Class<DeliveryRoutePo> poClass() {
        return DeliveryRoutePo.class;
    }
    
    /**
     * 根据路线名称查询
     */
    public List<DeliveryRoute> findByName(String routeName) {
        Criteria criteria = Criteria.where("routeName").is(routeName);
        Query query = new Query(criteria);
        List<DeliveryRoutePo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryRoutePo::toDO).toList();
    }
    
    /**
     * 根据状态查询配送路线
     */
    public List<DeliveryRoute> findByStatus(String status) {
        Criteria criteria = Criteria.where("status").is(status);
        Query query = new Query(criteria);
        List<DeliveryRoutePo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryRoutePo::toDO).toList();
    }
    
    /**
     * 根据路线类型查询
     */
    public List<DeliveryRoute> findByRouteType(String routeType) {
        Criteria criteria = Criteria.where("routeType").is(routeType);
        Query query = new Query(criteria);
        List<DeliveryRoutePo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryRoutePo::toDO).toList();
    }
    
    /**
     * 查询可用的配送路线
     */
    public List<DeliveryRoute> findAvailableRoutes() {
        Criteria criteria = Criteria.where("status").is("ACTIVE");
        Query query = new Query(criteria);
        List<DeliveryRoutePo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryRoutePo::toDO).toList();
    }
}

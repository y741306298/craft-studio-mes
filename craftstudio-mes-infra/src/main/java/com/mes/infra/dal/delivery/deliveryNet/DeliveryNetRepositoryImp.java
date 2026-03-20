package com.mes.infra.dal.delivery.deliveryNet;

import com.mes.domain.delivery.deliveryNet.entity.DeliveryNet;
import com.mes.domain.delivery.deliveryNet.repository.DeliveryNetRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.delivery.deliveryNet.po.DeliveryNetPo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DeliveryNetRepositoryImp extends BaseRepositoryImp<DeliveryNet, DeliveryNetPo> implements DeliveryNetRepository {
    
    @Override
    public Class<DeliveryNetPo> poClass() {
        return DeliveryNetPo.class;
    }
    
    /**
     * 根据配送网络名称查询
     */
    public List<DeliveryNet> findByName(String deliveryNetName) {
        Criteria criteria = Criteria.where("deliveryNetName").is(deliveryNetName);
        Query query = new Query(criteria);
        List<DeliveryNetPo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryNetPo::toDO).toList();
    }
    
    /**
     * 根据状态查询配送网络
     */
    public List<DeliveryNet> findByStatus(String status) {
        Criteria criteria = Criteria.where("status").is(status);
        Query query = new Query(criteria);
        List<DeliveryNetPo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryNetPo::toDO).toList();
    }
    
    /**
     * 根据覆盖类型查询
     */
    public List<DeliveryNet> findByCoverageType(String coverageType) {
        Criteria criteria = Criteria.where("coverageType").is(coverageType);
        Query query = new Query(criteria);
        List<DeliveryNetPo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryNetPo::toDO).toList();
    }
    
    /**
     * 查询可用的配送网络
     */
    public List<DeliveryNet> findAvailableNets() {
        Criteria criteria = Criteria.where("status").is("ACTIVE");
        Query query = new Query(criteria);
        List<DeliveryNetPo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryNetPo::toDO).toList();
    }
    
    /**
     * 根据路线 ID 查询
     */
    public List<DeliveryNet> findByRouteId(String routeId) {
        Criteria criteria = Criteria.where("routeId").is(routeId);
        Query query = new Query(criteria);
        List<DeliveryNetPo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryNetPo::toDO).toList();
    }
}

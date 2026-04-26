package com.mes.infra.dal.delivery.deliveryRoute;

import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNodeBinding;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteNodeBindingRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.delivery.deliveryRoute.po.DeliveryRouteNodeBindingPo;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DeliveryRouteNodeBindingRepositoryImp extends BaseRepositoryImp<DeliveryRouteNodeBinding, DeliveryRouteNodeBindingPo> implements DeliveryRouteNodeBindingRepository {

    @Override
    public Class<DeliveryRouteNodeBindingPo> poClass() {
        return DeliveryRouteNodeBindingPo.class;
    }

    @Override
    public List<DeliveryRouteNodeBinding> listByManufacturerAndTerminalRegion(String manufacturerMetaId, String terminalRegionCode) {
        Query query = new SoftDeleteQuery(
                Criteria.where("manufacturerMetaId").is(manufacturerMetaId)
                        .and("terminalRegionCode").is(terminalRegionCode)
        );
        List<DeliveryRouteNodeBindingPo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryRouteNodeBindingPo::toDO).toList();
    }

    @Override
    public DeliveryRouteNodeBinding findByManufacturerAndAddress(String manufacturerMetaId, String terminalRegionCode, String detailAddress) {
        Query query = new SoftDeleteQuery(
                Criteria.where("manufacturerMetaId").is(manufacturerMetaId)
                        .and("terminalRegionCode").is(terminalRegionCode)
                        .and("detailAddress").is(detailAddress)
        );
        DeliveryRouteNodeBindingPo po = mongoTemplate.findOne(query, poClass());
        return po == null ? null : po.toDO();
    }
}

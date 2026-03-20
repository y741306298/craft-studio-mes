package com.mes.infra.dal.delivery.deliveryPkg;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg;
import com.mes.domain.delivery.deliveryPkg.enums.DeliveryPkgStatus;
import com.mes.domain.delivery.deliveryPkg.repository.DeliveryPkgRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.delivery.deliveryPkg.po.DeliveryPkgPo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 包裹仓储实现类
 */
@Repository
public class DeliveryPkgRepositoryImp extends BaseRepositoryImp<DeliveryPkg, DeliveryPkgPo> implements DeliveryPkgRepository {

    @Override
    public Class<DeliveryPkgPo> poClass() {
        return DeliveryPkgPo.class;
    }

    /**
     * 根据包裹编码查询
     */
    @Override
    public List<DeliveryPkg> findByDeliveryPkgCode(String deliveryPkgCode) {
        Criteria criteria = Criteria.where("deliveryPkgCode").is(deliveryPkgCode);
        Query query = new Query(criteria);
        List<DeliveryPkgPo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryPkgPo::toDO).toList();
    }

    /**
     * 根据状态查询包裹
     */
    @Override
    public List<DeliveryPkg> findByStatus(DeliveryPkgStatus status) {
        Criteria criteria = Criteria.where("deliveryPkgStatus").is(status);
        Query query = new Query(criteria);
        List<DeliveryPkgPo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryPkgPo::toDO).toList();
    }

    /**
     * 根据运单号查询
     */
    @Override
    public List<DeliveryPkg> findByTrackingNumber(String trackingNumber) {
        Criteria criteria = Criteria.where("trackingNumber").is(trackingNumber);
        Query query = new Query(criteria);
        List<DeliveryPkgPo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryPkgPo::toDO).toList();
    }

    /**
     * 根据收件人姓名模糊查询
     */
    @Override
    public List<DeliveryPkg> findByRecipientName(String recipientName) {
        Criteria criteria = Criteria.where("recipientName").regex(recipientName, "i");
        Query query = new Query(criteria);
        List<DeliveryPkgPo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryPkgPo::toDO).toList();
    }

    /**
     * 查询待打包的包裹
     */
    @Override
    public List<DeliveryPkg> findPendingPacking() {
        Criteria criteria = Criteria.where("deliveryPkgStatus").is(DeliveryPkgStatus.PENDING_PACKING);
        Query query = new Query(criteria);
        List<DeliveryPkgPo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryPkgPo::toDO).toList();
    }

    /**
     * 查询已发货的包裹
     */
    @Override
    public List<DeliveryPkg> findDelivered() {
        Criteria criteria = Criteria.where("deliveryPkgStatus").is(DeliveryPkgStatus.DELIVERED);
        Query query = new Query(criteria);
        List<DeliveryPkgPo> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryPkgPo::toDO).toList();
    }
}

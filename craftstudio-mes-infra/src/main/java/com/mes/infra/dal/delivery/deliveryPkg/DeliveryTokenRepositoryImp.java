package com.mes.infra.dal.delivery.deliveryPkg;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryToken;
import com.mes.domain.delivery.deliveryPkg.repository.DeliveryTokenRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.delivery.deliveryPkg.po.DeliveryTokenPO;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 电子面单令牌仓储实现
 */
@Repository
public class DeliveryTokenRepositoryImp extends BaseRepositoryImp<DeliveryToken, DeliveryTokenPO> implements DeliveryTokenRepository {

    @Override
    public Class<DeliveryTokenPO> poClass() {
        return DeliveryTokenPO.class;
    }

    /**
     * 根据月结账号查询令牌配置
     */
    public List<DeliveryToken> findByPartnerId(String partnerId) {
        Criteria criteria = Criteria.where("partnerId").is(partnerId);
        Query query = new Query(criteria);
        List<DeliveryTokenPO> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryTokenPO::toDO).toList();
    }

    /**
     * 根据快递公司编码查询
     */
    public List<DeliveryToken> findByKuaidicom(String kuaidicom) {
        Criteria criteria = Criteria.where("kuaidicom").is(kuaidicom);
        Query query = new Query(criteria);
        List<DeliveryTokenPO> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryTokenPO::toDO).toList();
    }

    /**
     * 根据打印类型查询
     */
    public List<DeliveryToken> findByPrintType(String printType) {
        Criteria criteria = Criteria.where("printType").is(printType);
        Query query = new Query(criteria);
        List<DeliveryTokenPO> pos = mongoTemplate.find(query, poClass());
        return pos.stream().map(DeliveryTokenPO::toDO).toList();
    }

    @Override
    public DeliveryToken findByCarrierIdAndManufacturerMetaId(String carrierId, String manufacturerMetaId) {
        Criteria criteria = Criteria.where("carrierId").is(carrierId)
                .and("manufacturerMetaId").is(manufacturerMetaId);
        Query query = new Query(criteria);
        DeliveryTokenPO po = mongoTemplate.findOne(query, poClass());
        return po != null ? po.toDO() : null;
    }
}

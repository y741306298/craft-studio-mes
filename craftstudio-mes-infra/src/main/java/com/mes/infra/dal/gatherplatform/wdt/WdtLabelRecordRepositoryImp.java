package com.mes.infra.dal.gatherplatform.wdt;

import com.mes.domain.gatherplatform.wdt.entity.WdtLabelRecord;
import com.mes.domain.gatherplatform.wdt.repository.WdtLabelRecordRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.gatherplatform.wdt.po.WdtLabelRecordPO;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * 基于 MongoDB 的旺店通快递面单记录仓储实现。
 */
@Repository
public class WdtLabelRecordRepositoryImp extends BaseRepositoryImp<WdtLabelRecord, WdtLabelRecordPO> implements WdtLabelRecordRepository {
    /**
     * 返回当前仓储对应的 MongoDB 持久化类型。
     */
    @Override
    public Class<WdtLabelRecordPO> poClass() {
        return WdtLabelRecordPO.class;
    }
    @Override
    public WdtLabelRecord findForOrder(String orderId, String channelOrderId, String logisticsOrderId) {
        return findForPlatform("WDT", orderId, channelOrderId, logisticsOrderId);
    }

    @Override
    public WdtLabelRecord findForPlatform(String platformType, String orderId, String channelOrderId, String logisticsOrderId) {
        java.util.List<Criteria> keys = new java.util.ArrayList<>();
        if (orderId != null && !orderId.isBlank()) keys.add(Criteria.where("orderId").is(orderId));
        if (channelOrderId != null && !channelOrderId.isBlank()) keys.add(Criteria.where("channelOrderId").is(channelOrderId));
        if (logisticsOrderId != null && !logisticsOrderId.isBlank()) keys.add(Criteria.where("logisticsOrderId").is(logisticsOrderId));
        if (keys.isEmpty()) return null;
        Criteria platform = "WDT".equals(platformType)
                ? new Criteria().orOperator(Criteria.where("platformType").is("WDT"), Criteria.where("platformType").is(null))
                : Criteria.where("platformType").is(platformType);
        WdtLabelRecordPO po = mongoTemplate.findOne(new Query(new Criteria().andOperator(platform,
                new Criteria().orOperator(keys.toArray(new Criteria[0])), Criteria.where("deleteAt").is(null))), WdtLabelRecordPO.class);
        return po == null ? null : po.toDO();
    }

    @Override
    public WdtLabelRecord findByDeliveryPkgId(String deliveryPkgId) {
        WdtLabelRecordPO po = mongoTemplate.findOne(new Query(new Criteria().andOperator(
                Criteria.where("deliveryPkgId").is(deliveryPkgId), Criteria.where("deleteAt").is(null))),
                WdtLabelRecordPO.class);
        return po == null ? null : po.toDO();
    }
}

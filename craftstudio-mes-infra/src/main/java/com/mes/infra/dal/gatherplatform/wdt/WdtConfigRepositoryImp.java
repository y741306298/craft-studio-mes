package com.mes.infra.dal.gatherplatform.wdt;

import com.mes.domain.gatherplatform.wdt.entity.WdtConfig;
import com.mes.domain.gatherplatform.wdt.repository.WdtConfigRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.gatherplatform.wdt.po.WdtConfigPO;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * 基于 MongoDB 的旺店通快递配置仓储实现。
 */
@Repository
public class WdtConfigRepositoryImp extends BaseRepositoryImp<WdtConfig, WdtConfigPO> implements WdtConfigRepository {
    /**
     * 返回当前仓储对应的 MongoDB 持久化类型。
     */
    @Override
    public Class<WdtConfigPO> poClass() {
        return WdtConfigPO.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WdtConfig findByManufacturerMetaIdAndPresetType(String manufacturerMetaId, String presetType) {
        Query query = new Query(Criteria.where("manufacturerMetaId").is(manufacturerMetaId)
                .and("presetType").is(presetType));
        WdtConfigPO value = mongoTemplate.findOne(query, poClass());
        return value == null ? null : value.toDO();
    }
}

package com.mes.infra.dal.manufacurer.manufacturerMeta;

import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerDeviceCfgRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.manufacturerMeta.po.ManufacturerDeviceCfgPo;
import org.bson.Document;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class ManufacturerDeviceCfgRepositoryImp extends BaseRepositoryImp<ManufacturerDeviceCfg, ManufacturerDeviceCfgPo> implements ManufacturerDeviceCfgRepository {

    @Override
    public Class<ManufacturerDeviceCfgPo> poClass() {
        return ManufacturerDeviceCfgPo.class;
    }

    @Override
    public Map<String, Long> countByManufacturerMetaIds(Collection<String> manufacturerMetaIds) {
        if (manufacturerMetaIds == null || manufacturerMetaIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("manufacturerMetaId").in(manufacturerMetaIds)
                        .and("deleteAt").is(null)),
                Aggregation.group("manufacturerMetaId").count().as("count"));
        return mongoTemplate.aggregate(aggregation, poClass(), Document.class).getMappedResults().stream()
                .collect(Collectors.toMap(document -> document.getString("_id"),
                        document -> ((Number) document.get("count")).longValue()));
    }
}

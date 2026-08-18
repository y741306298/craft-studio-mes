package com.mes.infra.dal.manufacurer.typesetting;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.repository.TypesettingRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.typesetting.po.TypesettingPo;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Repository
public class TypesettingRepositoryImp extends BaseRepositoryImp<TypesettingInfo, TypesettingPo> implements TypesettingRepository {

    @Override
    public void batchUpdateCallbackFailure(Collection<String> ids, String status, String remark) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Object> mongoIds = new ArrayList<>(ids.size() * 2);
        for (String id : ids) {
            if (StringUtils.isBlank(id)) {
                continue;
            }
            mongoIds.add(id);
            if (ObjectId.isValid(id)) {
                mongoIds.add(new ObjectId(id));
            }
        }
        if (mongoIds.isEmpty()) {
            return;
        }
        Query query = new SoftDeleteQuery(Criteria.where("_id").in(mongoIds));
        Update update = new Update()
                .set("status", status)
                .set("remark", remark)
                .set("updateTime", new Date());
        mongoTemplate.updateMulti(query, update, poClass());
    }

    @Override
    public Class<TypesettingPo> poClass() {
        return TypesettingPo.class;
    }

    @Override
    public TypesettingInfo findById(String id) {
        TypesettingPo po = findPoById(id);
        return po == null ? null : po.toDO();
    }

    private TypesettingPo findPoById(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        TypesettingPo po = mongoTemplate.findOne(
                new SoftDeleteQuery(Criteria.where("_id").is(id)), poClass()
        );
        if (po != null || !ObjectId.isValid(id)) {
            return po;
        }
        return mongoTemplate.findOne(
                new SoftDeleteQuery(Criteria.where("_id").is(new ObjectId(id))), poClass()
        );
    }
}

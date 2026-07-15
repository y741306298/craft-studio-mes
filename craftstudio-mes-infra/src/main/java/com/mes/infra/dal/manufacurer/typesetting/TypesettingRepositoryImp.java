package com.mes.infra.dal.manufacurer.typesetting;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.repository.TypesettingRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.typesetting.po.TypesettingPo;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

@Repository
public class TypesettingRepositoryImp extends BaseRepositoryImp<TypesettingInfo, TypesettingPo> implements TypesettingRepository {

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

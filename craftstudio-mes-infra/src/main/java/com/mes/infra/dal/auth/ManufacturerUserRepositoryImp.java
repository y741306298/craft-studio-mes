package com.mes.infra.dal.auth;

import com.mes.domain.auth.entity.ManufacturerUser;
import com.mes.domain.auth.repository.ManufacturerUserRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.auth.po.ManufacturerUserPo;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

@Repository
public class ManufacturerUserRepositoryImp extends BaseRepositoryImp<ManufacturerUser, ManufacturerUserPo> implements ManufacturerUserRepository {
    @Override
    public Class<ManufacturerUserPo> poClass() {
        return ManufacturerUserPo.class;
    }

    @Override
    public ManufacturerUser findByAccount(String account) {
        ManufacturerUserPo po = mongoTemplate.findOne(
                new SoftDeleteQuery(Criteria.where("account").is(account)),
                ManufacturerUserPo.class
        );
        return po == null ? null : po.toDO();
    }
}

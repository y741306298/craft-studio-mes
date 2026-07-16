package com.mes.infra.dal.auth;

import com.mes.domain.auth.entity.ConfigUser;
import com.mes.domain.auth.repository.ConfigUserRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.auth.po.ConfigUserPo;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

@Repository
public class ConfigUserRepositoryImp extends BaseRepositoryImp<ConfigUser, ConfigUserPo> implements ConfigUserRepository {
    @Override
    public Class<ConfigUserPo> poClass() {
        return ConfigUserPo.class;
    }

    @Override
    public ConfigUser findByAccount(String account) {
        ConfigUserPo po = mongoTemplate.findOne(
                new SoftDeleteQuery(Criteria.where("account").is(account)),
                ConfigUserPo.class
        );
        return po == null ? null : po.toDO();
    }

    @Override
    public ConfigUser findByPhone(String phone) {
        ConfigUserPo po = mongoTemplate.findOne(
                new SoftDeleteQuery(Criteria.where("phone").is(phone)),
                ConfigUserPo.class
        );
        return po == null ? null : po.toDO();
    }
}

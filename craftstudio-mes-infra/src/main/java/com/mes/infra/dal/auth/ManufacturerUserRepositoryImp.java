package com.mes.infra.dal.auth;

import com.mes.domain.auth.entity.ManufacturerUser;
import com.mes.domain.auth.repository.ManufacturerUserRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.auth.po.ManufacturerUserPo;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    @Override
    public List<ManufacturerUser> listByManufacturerMetaId(String manufacturerMetaId, String phone, long current, int size) {
        Criteria criteria = Criteria.where("manufacturerMetaId").is(manufacturerMetaId);
        if (phone != null && !phone.trim().isEmpty()) {
            criteria.and("phone").regex(phone.trim(), "i");
        }
        List<ManufacturerUserPo> pos = mongoTemplate.find(
                new SoftDeleteQuery(criteria)
                        .with(Sort.by(Sort.Direction.DESC, "updateTime"))
                        .skip((current - 1) * size)
                        .limit(size),
                ManufacturerUserPo.class
        );
        return pos.stream().map(ManufacturerUserPo::toDO).toList();
    }

    @Override
    public long totalByManufacturerMetaId(String manufacturerMetaId, String phone) {
        Criteria criteria = Criteria.where("manufacturerMetaId").is(manufacturerMetaId);
        if (phone != null && !phone.trim().isEmpty()) {
            criteria.and("phone").regex(phone.trim(), "i");
        }
        return mongoTemplate.count(
                new SoftDeleteQuery(criteria),
                ManufacturerUserPo.class
        );
    }
}

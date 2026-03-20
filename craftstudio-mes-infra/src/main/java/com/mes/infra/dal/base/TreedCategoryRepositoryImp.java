package com.mes.infra.dal.base;


import com.mes.domain.base.TreedCategory;
import com.mes.domain.base.repository.TreedCategoryRepository;
import com.mes.infra.base.BasePO;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

public abstract class TreedCategoryRepositoryImp<DO extends TreedCategory, PO extends BasePO<DO>> extends BaseRepositoryImp<DO, PO> implements TreedCategoryRepository<DO> {

    @Override
    public List<DO> findFirstLevelCategoryList() {
        List<PO> pos = mongoTemplate.find(
            new SoftDeleteQuery(Criteria.where("parentId").is(null)), poClass()
        );
        return pos.stream().map(BasePO::toDO).toList();
    }

    @Override
    public List<DO> findByParentId(String parentId) {
        List<PO> pos = mongoTemplate.find(
            new SoftDeleteQuery(Criteria.where("parentId").is(parentId)), poClass()
        );
        return pos.stream().map(BasePO::toDO).toList();
    }

    @Override
    public boolean existByParentId(String parentId) {
        return mongoTemplate.exists(new SoftDeleteQuery(
                Criteria.where("parentId").is(parentId)
        ), poClass());
    }
}

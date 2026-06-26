package com.mes.infra.dal.manufacurer.materialLayoutSpec;

import com.mes.domain.manufacturer.materialLayoutSpec.entity.MaterialLayoutSpec;
import com.mes.domain.manufacturer.materialLayoutSpec.repository.MaterialLayoutSpecRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.materialLayoutSpec.po.MaterialLayoutSpecPo;
import org.springframework.stereotype.Repository;

@Repository
public class MaterialLayoutSpecRepositoryImp extends BaseRepositoryImp<MaterialLayoutSpec, MaterialLayoutSpecPo>
        implements MaterialLayoutSpecRepository {

    @Override
    public Class<MaterialLayoutSpecPo> poClass() {
        return MaterialLayoutSpecPo.class;
    }
}

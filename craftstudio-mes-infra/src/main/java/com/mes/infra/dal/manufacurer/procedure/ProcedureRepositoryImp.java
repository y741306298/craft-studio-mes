package com.mes.infra.dal.manufacurer.procedure;

import com.mes.domain.manufacturer.procedure.entity.Procedure;
import com.mes.domain.manufacturer.procedure.repository.ProcedureRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.procedure.po.ProcedurePo;
import org.springframework.stereotype.Repository;

@Repository
public class ProcedureRepositoryImp extends BaseRepositoryImp<Procedure, ProcedurePo> implements ProcedureRepository {

    @Override
    public Class<ProcedurePo> poClass() {
        return ProcedurePo.class;
    }
}

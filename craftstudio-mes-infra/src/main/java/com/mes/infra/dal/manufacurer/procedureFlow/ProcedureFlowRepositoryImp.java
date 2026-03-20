package com.mes.infra.dal.manufacurer.procedureFlow;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.repository.ProcedureFlowRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.manufacurer.procedureFlow.po.ProcedureFlowRepositoryPo;
import org.springframework.stereotype.Repository;

@Repository
public class ProcedureFlowRepositoryImp extends BaseRepositoryImp<ProcedureFlow, ProcedureFlowRepositoryPo> implements ProcedureFlowRepository {
    
    @Override
    public Class<ProcedureFlowRepositoryPo> poClass() {
        return ProcedureFlowRepositoryPo.class;
    }
}

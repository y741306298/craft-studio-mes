package com.mes.infra.dal.order.preOrderLabelTask;

import com.mes.domain.order.preOrderLabelTask.entity.PreOrderLabelTask;
import com.mes.domain.order.preOrderLabelTask.repository.PreOrderLabelTaskRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.order.preOrderLabelTask.po.PreOrderLabelTaskPo;
import org.springframework.stereotype.Repository;

@Repository
public class PreOrderLabelTaskRepositoryImp extends BaseRepositoryImp<PreOrderLabelTask, PreOrderLabelTaskPo> implements PreOrderLabelTaskRepository {
    @Override
    public Class<PreOrderLabelTaskPo> poClass() {
        return PreOrderLabelTaskPo.class;
    }
}

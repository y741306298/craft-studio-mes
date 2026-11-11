package com.mes.infra.dal.order.sublimationTrace;

import com.mes.domain.order.orderInfo.entity.SublimationTraceOrder;
import com.mes.domain.order.orderInfo.repository.SublimationTraceOrderRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.order.sublimationTrace.po.SublimationTraceOrderPo;
import org.springframework.stereotype.Repository;

@Repository
public class SublimationTraceOrderRepositoryImp extends BaseRepositoryImp<SublimationTraceOrder, SublimationTraceOrderPo>
        implements SublimationTraceOrderRepository {
    @Override public Class<SublimationTraceOrderPo> poClass() { return SublimationTraceOrderPo.class; }
}

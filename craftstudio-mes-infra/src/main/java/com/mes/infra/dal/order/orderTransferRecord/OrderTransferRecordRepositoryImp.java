package com.mes.infra.dal.order.orderTransferRecord;

import com.mes.domain.order.orderTransferRecord.entity.OrderTransferRecord;
import com.mes.domain.order.orderTransferRecord.repository.OrderTransferRecordRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.order.orderTransferRecord.po.OrderTransferRecordPo;
import org.springframework.stereotype.Repository;

@Repository
public class OrderTransferRecordRepositoryImp extends BaseRepositoryImp<OrderTransferRecord, OrderTransferRecordPo> implements OrderTransferRecordRepository {

    @Override
    public Class<OrderTransferRecordPo> poClass() {
        return OrderTransferRecordPo.class;
    }
}

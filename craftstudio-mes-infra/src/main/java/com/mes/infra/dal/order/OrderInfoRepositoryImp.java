package com.mes.infra.dal.order;

import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.repository.OrderInfoRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.order.po.OrderInfoPo;
import org.springframework.stereotype.Repository;

@Repository
public class OrderInfoRepositoryImp extends BaseRepositoryImp<OrderInfo, OrderInfoPo> implements OrderInfoRepository {

    @Override
    public Class<OrderInfoPo> poClass() {
        return OrderInfoPo.class;
    }
}

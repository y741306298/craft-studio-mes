package com.mes.infra.dal.odder;

import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.repository.OrderItemRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.order.po.OrderItemPo;
import org.springframework.stereotype.Repository;

@Repository
public class OrderItemRepositoryImp extends BaseRepositoryImp<OrderItem, OrderItemPo> implements OrderItemRepository {

    @Override
    public Class<OrderItemPo> poClass() {
        return OrderItemPo.class;
    }
}

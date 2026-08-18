package com.mes.infra.dal.order.orderItemPriceAllocation;

import com.mes.domain.order.orderItemPriceAllocation.entity.OrderItemPriceAllocation;
import com.mes.domain.order.orderItemPriceAllocation.repository.OrderItemPriceAllocationRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.order.orderItemPriceAllocation.po.OrderItemPriceAllocationPo;
import org.springframework.stereotype.Repository;

@Repository
public class OrderItemPriceAllocationRepositoryImp
        extends BaseRepositoryImp<OrderItemPriceAllocation, OrderItemPriceAllocationPo>
        implements OrderItemPriceAllocationRepository {
    @Override
    public Class<OrderItemPriceAllocationPo> poClass() {
        return OrderItemPriceAllocationPo.class;
    }
}

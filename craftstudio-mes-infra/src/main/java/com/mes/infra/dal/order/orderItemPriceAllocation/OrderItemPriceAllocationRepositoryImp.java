package com.mes.infra.dal.order.orderItemPriceAllocation;

import com.mes.domain.order.orderItemPriceAllocation.entity.OrderItemPriceAllocation;
import com.mes.domain.order.orderItemPriceAllocation.repository.OrderItemPriceAllocationRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.order.orderItemPriceAllocation.po.OrderItemPriceAllocationPo;
import com.mes.infra.db.mongodb.SoftDeleteQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

@Repository
public class OrderItemPriceAllocationRepositoryImp
        extends BaseRepositoryImp<OrderItemPriceAllocation, OrderItemPriceAllocationPo>
        implements OrderItemPriceAllocationRepository {
    @Override
    public Class<OrderItemPriceAllocationPo> poClass() {
        return OrderItemPriceAllocationPo.class;
    }

    @Override
    public OrderItemPriceAllocation findByOrderItemIdAndManufacturerMetaId(String orderItemId,
                                                                           String manufacturerMetaId) {
        OrderItemPriceAllocationPo po = mongoTemplate.findOne(
                new SoftDeleteQuery(Criteria.where("orderItemId").is(orderItemId)
                        .and("manufacturerMetaId").is(manufacturerMetaId)),
                poClass());
        return po == null ? null : po.toDO();
    }
}

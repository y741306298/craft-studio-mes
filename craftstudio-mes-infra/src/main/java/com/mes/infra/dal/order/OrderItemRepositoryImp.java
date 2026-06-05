package com.mes.infra.dal.order;

import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.repository.OrderItemRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.order.po.OrderItemPo;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class OrderItemRepositoryImp extends BaseRepositoryImp<OrderItem, OrderItemPo> implements OrderItemRepository {

    @Override
    public Class<OrderItemPo> poClass() {
        return OrderItemPo.class;
    }

    @Override
    public List<OrderItem> filterListUrgentFirst(long current, int size, Map<String, Object> filters) {
        return filterList(
                current,
                size,
                filters,
                Sort.by(
                        Sort.Order.desc("isUrgent"),
                        Sort.Order.desc("updateTime")
                )
        );
    }
}

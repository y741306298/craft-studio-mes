package com.mes.infra.dal.order.sublimationTrace.po;

import com.mes.domain.order.orderInfo.entity.SublimationTraceOrder;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "sublimation_trace_order")
public class SublimationTraceOrderPo extends BasePO<SublimationTraceOrder> {
    private String orderId;
    private String manufacturerId;
    private Integer sequence;
    private List<String> orderItemIds;

    @Override public SublimationTraceOrder toDO() {
        SublimationTraceOrder order = new SublimationTraceOrder();
        copyBaseFieldsToDO(order);
        order.setOrderId(orderId);
        order.setManufacturerId(manufacturerId);
        order.setSequence(sequence);
        order.setOrderItemIds(orderItemIds);
        return order;
    }

    @Override protected BasePO<SublimationTraceOrder> fromDO(SublimationTraceOrder order) {
        orderId = order.getOrderId();
        manufacturerId = order.getManufacturerId();
        sequence = order.getSequence();
        orderItemIds = order.getOrderItemIds();
        return this;
    }
}

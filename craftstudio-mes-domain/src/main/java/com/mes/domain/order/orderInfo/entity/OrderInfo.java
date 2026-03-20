package com.mes.domain.order.orderInfo.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.order.orderInfo.vo.OrderCustomer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderInfo extends BaseEntity {

    private String orderId;
    private OrderCustomer customer;
    private String deliveryAddress;
    private String status;
    private List<String> itemIds;

}

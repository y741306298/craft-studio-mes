package com.mes.application.dto.req.order;


import com.mes.domain.order.enums.OrderStatus;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 订单新增请求 DTO
 */
@Data
public class OrderAddRequest {
    private List<OrderItemRequest> orderItems;
    private ConsigneeRequest consignee;
    private Long id;
    private String state;


    public OrderInfo toOrderInfo() {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderId(String.valueOf(id));
        orderInfo.setCustomer(consignee.toOrderCustomer());
        orderInfo.setDeliveryAddress(consignee.getDetailAddress());
        orderInfo.setStatus(OrderStatus.PENDING);
        orderInfo.setExpectedDeliveryDate(new Date());
        return orderInfo;
    }

    public List<OrderItem> toOrderItems() {
        List<OrderItem> orderItems = new ArrayList<OrderItem>();
        for (OrderItemRequest orderItemRequest : this.orderItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderItemId(String.valueOf(orderItemRequest.getId()));
            orderItem.setMtoProduct(orderItemRequest.getMtoProductSpec());
            orderItem.setManufacturerId(orderItemRequest.getSpecifyRmfInfo().getRmfId());
            orderItem.setQuantity(orderItemRequest.getCount());
            orderItem.setStatus(OrderStatus.PENDING);
            orderItem.setIsUrgent(false);
            orderItem.setKuaidiWay(orderItemRequest.getLogisticsCarrierInfo().getCarrierId());
            orderItems.add(orderItem);
        }
        return orderItems;
    }

}



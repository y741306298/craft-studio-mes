package com.mes.application.dto.resp.order;

import com.mes.application.command.order.vo.OrderWithItemsVO;
import lombok.Data;

import java.util.List;

/**
 * 订单及订单项响应 DTO
 */
@Data
public class OrderWithItemsResponse {
    private OrderInfoResponse orderInfo;
    private List<OrderItemResponse> orderItems;

    public static OrderWithItemsResponse from(OrderWithItemsVO vo) {
        OrderWithItemsResponse response = new OrderWithItemsResponse();
        response.setOrderInfo(OrderInfoResponse.from(vo.getOrderInfo()));
        response.setOrderItems(vo.getOrderItems().stream()
                .map(OrderItemResponse::from)
                .toList());
        return response;
    }
}
package com.mes.interfaces.api.dto.resp.order;

import com.mes.domain.order.orderInfo.entity.OrderItem;
import lombok.Data;

/**
 * 订单项响应 DTO
 */
@Data
public class OrderItemResponse {
    private String id;
    private String orderItemId;
    private String orderId;
    private Object mtoProduct;
    private String material;
    private String procedureFlowId;
    private Integer quantity;
    private String status;
    private Boolean isUrgent;
    private String processingFlow;
    private Object productionImgFile;
    private Object maskImgFile;
    private String failureReason;

    public static OrderItemResponse from(OrderItem orderItem) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(orderItem.getId());
        response.setOrderItemId(orderItem.getOrderItemId());
        response.setOrderId(orderItem.getOrderId());
        response.setMtoProduct(orderItem.getMtoProduct());
        response.setMaterial(orderItem.getMaterial());
        response.setQuantity(orderItem.getQuantity());
        response.setStatus(orderItem.getStatus() != null ? orderItem.getStatus().getCode() : null);
        response.setIsUrgent(orderItem.getIsUrgent());
        response.setProcessingFlow(orderItem.getProcessingFlow());
        response.setProductionImgFile(orderItem.getProductionImgFile());
        response.setMaskImgFile(orderItem.getMaskImgFile());
        response.setFailureReason(orderItem.getFailureReason());
        return response;
    }
}
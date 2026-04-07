package com.mes.application.dto.resp.order;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import lombok.Data;

/**
 * 订单项响应 DTO
 */
@Data
public class OrderItemResponse {
    private String id;
    private String orderItemId;
    private String orderId;
    private ProcedureFlow procedureFlow;
    private MaterialConfig material;
    private String procedureFlowId;
    private Integer quantity;
    private String status;
    private Boolean isUrgent;
    private String processingFlow;
    private Object productionImgFile;
    private Object maskImgFile;
    private String failureReason;
    private java.util.List<ProductionPiece> productionPieces;

    public static OrderItemResponse from(OrderItem orderItem) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(orderItem.getId());
        response.setOrderItemId(orderItem.getOrderItemId());
        response.setOrderId(orderItem.getOrderId());
        response.setProcedureFlow(orderItem.getProcedureFlow());
        response.setMaterial(orderItem.getMaterial());
        response.setQuantity(orderItem.getQuantity());
        response.setStatus(orderItem.getStatus() != null ? orderItem.getStatus().getCode() : null);
        response.setIsUrgent(orderItem.getIsUrgent());
        response.setProcessingFlow(orderItem.getProcessingFlow());
        response.setProductionImgFile(orderItem.getProductionImgFile());
        response.setMaskImgFile(orderItem.getMaskImgFile());
        response.setFailureReason(orderItem.getFailureReason());
        response.setProductionPieces(orderItem.getProductionPieces());
        return response;
    }
}
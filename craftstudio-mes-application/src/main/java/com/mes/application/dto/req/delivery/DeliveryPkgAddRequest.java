package com.mes.application.dto.req.delivery;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import lombok.Data;

import java.util.List;

@Data
public class DeliveryPkgAddRequest {

    private List<DeliveryPkgPieceItem> pieces;
    private String deliveryManId;
    private String deliverySiidId;
    /** 快递100云打印设备编码，保存为包裹默认重打设备 */
    private String siid;
    private String carrierId;
    private String manufacturerMetaId;
    private String routeId;
    private String routeNodeId;

    @Data
    public static class DeliveryPkgPieceItem {
        private String productionPieceId;
        /**
         * Legacy request compatibility. New clients should send productionPieceId
         * directly; old clients may still send piece.productionPieceId.
         */
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private DeliveryPkgPieceVO piece;
        private Integer quantity;

        public String getProductionPieceId() {
            if (productionPieceId != null && !productionPieceId.isBlank()) {
                return productionPieceId;
            }
            return piece == null ? null : piece.getProductionPieceId();
        }
    }
}

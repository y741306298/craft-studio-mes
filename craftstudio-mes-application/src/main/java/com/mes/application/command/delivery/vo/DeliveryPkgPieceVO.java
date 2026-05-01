package com.mes.application.command.delivery.vo;

import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.vo.LogisticsCarrierInfo;
import com.mes.domain.order.orderInfo.vo.OrderCustomer;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import lombok.Data;

@Data
public class DeliveryPkgPieceVO {

    private String productionPieceId;
    private String orderItemId;
    private String orderId;
    private Integer quantity;
    private Integer pendingPkgQuantity;
    private Integer packedQuantity;
    private String status;
    private String previewUrl;
    private MaterialConfig materialConfig;
    private LogisticsCarrierInfo logisticsCarrierInfo;
    private OrderCustomer orderCustomer;

    public static DeliveryPkgPieceVO fromProductionPiece(ProductionPiece piece) {
        DeliveryPkgPieceVO vo = new DeliveryPkgPieceVO();
        vo.setProductionPieceId(piece.getProductionPieceId());
        vo.setOrderItemId(piece.getOrderItemId());
        vo.setQuantity(piece.getQuantity());
        vo.setMaterialConfig(piece.getMaterialConfig());
        if (piece.getProductImageFile() != null && piece.getProductImageFile().getFilePreview() != null) {
            vo.setPreviewUrl(piece.getProductImageFile().getFilePreview().getPreview());
        }
        return vo;
    }
}

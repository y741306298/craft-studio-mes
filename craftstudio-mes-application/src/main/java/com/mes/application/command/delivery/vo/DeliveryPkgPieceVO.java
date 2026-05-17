package com.mes.application.command.delivery.vo;

import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.order.orderInfo.vo.LogisticsCarrierInfo;
import com.mes.domain.order.orderInfo.vo.OrderCustomer;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import com.piliofpala.craftstudio.shared.domain.geo.world.repository.WorldRepository;
import com.piliofpala.craftstudio.shared.domain.geo.world.vo.World;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

@Data
public class DeliveryPkgPieceVO {

    private String productionPieceId;
    private String orderItemId;
    private String orderId;
    private Integer quantity;
    private Integer pendingPkgQuantity;
    private Integer packedQuantity;
    private String address;
    private String status;
    private String previewUrl;
    private java.util.Date createTime;
    private Double width;
    private Double height;
    private MaterialConfig materialConfig;
    private ProcedureFlow procedureFlow;
    private LogisticsCarrierInfo logisticsCarrierInfo;
    private OrderCustomer orderCustomer;
    private float score;

    public static DeliveryPkgPieceVO fromProductionPiece(ProductionPiece piece) {
        DeliveryPkgPieceVO vo = new DeliveryPkgPieceVO();
        vo.setProductionPieceId(piece.getProductionPieceId());
        vo.setOrderItemId(piece.getOrderItemId());
        vo.setQuantity(piece.getQuantity());
        vo.setMaterialConfig(piece.getMaterialConfig());
        vo.setProcedureFlow(piece.getProcedureFlow());
        vo.setCreateTime(piece.getCreateTime());
        vo.setWidth(piece.getWidth());
        vo.setHeight(piece.getHeight());
        if (piece.getProductImageFile() != null && piece.getProductImageFile().getFilePreview() != null) {
            vo.setPreviewUrl(piece.getProductImageFile().getFilePreview().getPreview());
        }
        return vo;
    }
}

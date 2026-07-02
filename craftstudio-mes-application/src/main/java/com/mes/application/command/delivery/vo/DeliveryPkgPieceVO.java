package com.mes.application.command.delivery.vo;

import com.mes.domain.delivery.deliveryRoute.vo.OrgInfo;
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
    private String routeId;
    private String routeNodeId;
    private Integer quantity;
    private Integer pendingPkgQuantity;
    private Integer packedQuantity;
    private String address;
    private String status;
    private Boolean isUrgent;
    private String previewUrl;
    private java.util.Date createTime;
    private Double width;
    private Double height;
    private MaterialConfig materialConfig;
    private ProcedureFlow procedureFlow;
    private String processingFlow;
    private LogisticsCarrierInfo logisticsCarrierInfo;
    private OrderCustomer orderCustomer;
    private OrgInfo orgInfo;
    private float score;
    private String remark;

    public static DeliveryPkgPieceVO fromProductionPiece(ProductionPiece piece) {
        DeliveryPkgPieceVO vo = new DeliveryPkgPieceVO();
        vo.setProductionPieceId(piece.getProductionPieceId());
        vo.setOrderItemId(piece.getOrderItemId());
        vo.setRouteId(piece.getRouteId());
        vo.setRouteNodeId(piece.getRouteNodeId());
        vo.setQuantity(piece.getQuantity());
        vo.setIsUrgent(piece.getIsUrgent());
        vo.setMaterialConfig(piece.getMaterialConfig());
        vo.setProcedureFlow(piece.getProcedureFlow());
        vo.setProcessingFlow(piece.getProcessingFlow());
        vo.setCreateTime(piece.getCreateTime());
        vo.setWidth(piece.getWidth());
        vo.setHeight(piece.getHeight());
        if (piece.getProductImageFile() != null && piece.getProductImageFile().getFilePreview() != null) {
            vo.setPreviewUrl(piece.getProductImageFile().getFilePreview().getPreview());
        }
        vo.setRemark(piece.getRemark());
        vo.setOrgInfo(piece.getOrgInfo());
        return vo;
    }
}

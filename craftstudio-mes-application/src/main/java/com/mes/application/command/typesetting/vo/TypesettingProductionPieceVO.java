package com.mes.application.command.typesetting.vo;

import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import lombok.Data;

import java.util.List;

/**
 * 排版与生产工件统一返回对象
 */
@Data
public class TypesettingProductionPieceVO {

    /**
     * 订单项Id
     */
    private String orderItemId;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 已完成数量
     */
    private Integer completedQuantity;

    /**
     * 材质
     */
    private String material;
    private String materialCode;

    /**
     * 工艺流程
     */
    private String processingFlow;

    /**
     * 预览 URL
     */
    private String previewUrl;

    /**
     * 备注
     */
    private String remark;

    /**
     * 来源类型TypesettingSourceType
     */
    private String sourceType;

    /**
     * 来源 ID
     */
    private String sourceId;

    /**
     * 零件状态ProductionPieceStatus
     */
    private String status;

    public static TypesettingProductionPieceVO fromPiece(ProductionPiece piece){
        TypesettingProductionPieceVO typesettingProductionPieceVO = new TypesettingProductionPieceVO();
        List<ProcedureFlowNode> nodes = piece.getProcedureFlow().getNodes();
        for (ProcedureFlowNode node : nodes) {
            if (node.getNodeName().equals("排版")) {
                typesettingProductionPieceVO.setQuantity(node.getPieceQuantity());
            }
        }
        typesettingProductionPieceVO.setOrderItemId(piece.getOrderItemId());
        typesettingProductionPieceVO.setMaterial(piece.getMaterialConfig().getMaterialSnapshot().getName());
        typesettingProductionPieceVO.setMaterialCode(piece.getMaterialConfig().getMaterialId());
        typesettingProductionPieceVO.setProcessingFlow(piece.getProcessingFlow());
        if(piece.getProductImageFile() != null) typesettingProductionPieceVO.setPreviewUrl(piece.getProductImageFile().getFilePreview().getPreview());
        typesettingProductionPieceVO.setSourceType(TypesettingSourceType.PART.getCode());
        typesettingProductionPieceVO.setSourceId(piece.getProductionPieceId());
        typesettingProductionPieceVO.setStatus(ProductionPieceStatus.PENDING_TYPESITTING.getCode());
        return typesettingProductionPieceVO;
    }
}

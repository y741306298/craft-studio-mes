package com.mes.application.command.typesetting.vo;

import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import lombok.Data;

import java.util.ArrayList;
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

    /**
     * 排版轮廓 SVG（排版来源）
     */
    private String maskSvg;

    /**
     * 排版方式
     */
    private String layoutMode;

    /**
     * 排版记录物料编码
     */
    private List<String> materialConfigs;

    /**
     * 生产工件模板 SVG
     */
    private String templateCode;

    public static TypesettingProductionPieceVO fromProductionPiece(ProductionPiece piece){
        TypesettingProductionPieceVO typesettingProductionPieceVO = new TypesettingProductionPieceVO();
        if (piece == null) {
            return typesettingProductionPieceVO;
        }
        if (piece.getProcedureFlow() != null && piece.getProcedureFlow().getNodes() != null) {
            List<ProcedureFlowNode> nodes = piece.getProcedureFlow().getNodes();
            for (ProcedureFlowNode node : nodes) {
                if ("排版".equals(node.getNodeName())) {
                    typesettingProductionPieceVO.setQuantity(node.getPieceQuantity());
                }
            }
        }
        typesettingProductionPieceVO.setOrderItemId(piece.getOrderItemId());
        if (piece.getMaterialConfig() != null) {
            if (piece.getMaterialConfig().getMaterialSnapshot() != null) {
                typesettingProductionPieceVO.setMaterial(piece.getMaterialConfig().getMaterialSnapshot().getName());
            }
            typesettingProductionPieceVO.setMaterialCode(piece.getMaterialConfig().getMaterialId());
        }
        typesettingProductionPieceVO.setProcessingFlow(piece.getProcessingFlow());
        if(piece.getProductImageFile() != null) typesettingProductionPieceVO.setPreviewUrl(piece.getProductImageFile().getFilePreview().getPreview());
        typesettingProductionPieceVO.setSourceType(TypesettingSourceType.PART.getCode());
        typesettingProductionPieceVO.setSourceId(piece.getProductionPieceId());
        typesettingProductionPieceVO.setTemplateCode(piece.getTemplateCode());
        typesettingProductionPieceVO.setStatus(ProductionPieceStatus.PENDING_TYPESITTING.getCode());
        return typesettingProductionPieceVO;
    }

    public static TypesettingProductionPieceVO fromPiece(ProductionPiece piece){
        return fromProductionPiece(piece);
    }

    public static TypesettingProductionPieceVO fromTypesettingInfo(TypesettingInfo info) {
        TypesettingProductionPieceVO vo = new TypesettingProductionPieceVO();
        if (info == null) {
            return vo;
        }
        vo.setSourceType(TypesettingSourceType.TYPESETTING.getCode());
        vo.setSourceId(info.getId());
        vo.setQuantity(info.getQuantity());
        vo.setCompletedQuantity(info.getCompletedQuantity());
        vo.setMaterialConfigs(info.getMaterialConfigs());
        vo.setMaterialCode(info.getMaterialConfigs() == null ? null : info.getMaterialConfigs().toString());
        vo.setStatus(info.getStatus());
        vo.setRemark(info.getRemark());
        vo.setMaskSvg(info.getMaskSvg());
        vo.setLayoutMode(info.getLayoutMode());
        return vo;
    }

    public ProductionPiece toProductionPiece() {
        ProductionPiece piece = new ProductionPiece();
        piece.setProductionPieceId(this.sourceId);
        piece.setOrderItemId(this.orderItemId);
        piece.setQuantity(this.quantity);
        piece.setTemplateCode(this.templateCode);
        piece.setStatus(this.status);
        return piece;
    }

    public TypesettingInfo toTypesettingInfo() {
        TypesettingInfo info = new TypesettingInfo();
        info.setId(this.sourceId);
        info.setTypesettingId(this.sourceId);
        info.setQuantity(this.quantity);
        info.setCompletedQuantity(this.completedQuantity);
        info.setMaterialConfigs(this.materialConfigs == null ? new ArrayList<>() : this.materialConfigs);
        info.setStatus(this.status);
        info.setRemark(this.remark);
        info.setMaskSvg(this.maskSvg);
        info.setLayoutMode(this.layoutMode);
        return info;
    }
}

package com.mes.application.command.typesetting.vo;

import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 排版与生产工件统一返回对象
 */
@Data
public class TypesettingProductionPieceVO {

    private String id;

    /**
     * 订单项Id
     */
    private String orderItemId;

    /**
     * 分组ID：生产工件用orderItemId，排版记录用typesettingId
     */
    private String groupId;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 剩余数量
     */
    private Integer leaveQuantity;

    /**
     * 材质配置
     */
    private MaterialConfig materialConfig;

    /**
     * Mirror plate original material name before materialConfig is replaced by accessory material.
     */
    private String oriName;

    /**
     * Mirror plate original material id before materialConfig is replaced by accessory material.
     */
    private String oriMaterialId;

    /**
     * Mirror plate original material type before materialConfig is replaced by accessory material.
     */
    private String oriMaterialType;

    /**
     * 工艺流程
     */
    private String processingFlow;

    /**
     * 工序流
     */
    private ProcedureFlow procedureFlow;

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
     * 是否加急
     */
    private Boolean isUrgent;

    /**
     * 是否重做（仅生产工件来源）
     */
    private Boolean isRedo;

    /**
     * 来源是否包含血位。toLayout 将该快照写入 Redis，供异步回调直接使用；
     * null 表示旧缓存没有该字段，回调需要回退到数据库查询。
     */
    private Boolean haveBlood;

    /**
     * 排版轮廓 SVG（排版来源）
     */
    private String maskSvg;

    /**
     * 排版方式
     */
    private String layoutMode;

    /**
     * 排版方式描述
     */
    private String description;

    /**
     * 排版记录物料编码
     */
    private List<String> materialConfigs;

    /**
     * 印版包含的来源单元；零件来源时为空。
     */
    private List<TypesettingSourceCell> typesettingCells;

    /**
     * 生产工件模板 SVG
     */
    private String templateCode;

    /**
     * 宽度
     */
    private Double width;

    /**
     * 高度
     */
    private Double height;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    public static TypesettingProductionPieceVO fromProductionPiece(ProductionPiece piece){
        TypesettingProductionPieceVO typesettingProductionPieceVO = new TypesettingProductionPieceVO();
        if (piece == null) {
            return typesettingProductionPieceVO;
        }
        if (piece.getProcedureFlow() != null && piece.getProcedureFlow().getNodes() != null) {
            List<ProcedureFlowNode> nodes = piece.getProcedureFlow().getNodes();
            for (ProcedureFlowNode node : nodes) {
                if ("待排版".equals(node.getNodeName())) {
                    typesettingProductionPieceVO.setQuantity(node.getPieceQuantity());
                    typesettingProductionPieceVO.setLeaveQuantity(node.getPieceQuantity());
                }
            }
        }
        typesettingProductionPieceVO.setOrderItemId(piece.getOrderItemId());
        typesettingProductionPieceVO.setGroupId(piece.getOrderItemId());
        typesettingProductionPieceVO.setMaterialConfig(piece.getMaterialConfig());
        typesettingProductionPieceVO.setProcessingFlow(piece.getProcessingFlow());
        typesettingProductionPieceVO.setProcedureFlow(piece.getProcedureFlow());
        if(piece.getProductImageFile() != null) typesettingProductionPieceVO.setPreviewUrl(piece.getProductImageFile().getFilePreview().getPreview());
        typesettingProductionPieceVO.setSourceType(TypesettingSourceType.PART.getCode());
        typesettingProductionPieceVO.setSourceId(piece.getId());
        typesettingProductionPieceVO.setId(piece.getId());
        typesettingProductionPieceVO.setTemplateCode(piece.getTemplateCode());
        typesettingProductionPieceVO.setWidth(toCentimeters(resolveDisplayWidth(piece)));
        typesettingProductionPieceVO.setHeight(toCentimeters(resolveDisplayHeight(piece)));
        typesettingProductionPieceVO.setCreateTime(piece.getCreateTime());
        typesettingProductionPieceVO.setIsUrgent(piece.getIsUrgent());
        typesettingProductionPieceVO.setIsRedo(piece.getIsRedo());
        typesettingProductionPieceVO.setStatus(ProductionPieceStatus.PENDING_TYPESITTING.getCode());
        typesettingProductionPieceVO.setRemark(piece.getRemark());
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
        vo.setId(info.getId());
        vo.setGroupId(info.getTypesettingId());
        vo.setQuantity(info.getQuantity());
        vo.setLeaveQuantity(info.getLeaveQuantity());
        vo.setMaterialConfigs(info.getMaterialConfigs());
        vo.setTypesettingCells(info.getTypesettingCells());
        vo.setMaterialConfig(info.getMaterialConfig());
        vo.setOriName(info.getOriName());
        vo.setOriMaterialId(info.getOriMaterialId());
        vo.setOriMaterialType(info.getOriMaterialType());
        vo.setProcessingFlow(info.getProcessingFlow());
        vo.setProcedureFlow(info.getProcedureFlow());
        vo.setPreviewUrl(info.getElement().getFormeSvg());
        vo.setStatus(info.getStatus());
        vo.setIsUrgent(info.getIsUrgent());
        vo.setHaveBlood(info.getHaveBlood());
        vo.setRemark(info.getRemark());
        vo.setMaskSvg(info.getMaskSvg());
        vo.setLayoutMode(info.getLayoutMode());
        vo.setDescription(resolveLayoutModeDescription(info));
        if (info.getElement() != null) {
            vo.setWidth(toCentimeters(info.getElement().getWidth().doubleValue()));
            vo.setHeight(toCentimeters(info.getElement().getHeight().doubleValue()));
        }
        vo.setCreateTime(info.getCreateTime());
        return vo;
    }

    private static String resolveLayoutModeDescription(TypesettingInfo info) {
        if (info.getDescription() != null) {
            return info.getDescription();
        }
        if (info.getLayoutMode() == null || info.getLayoutMode().trim().isEmpty()) {
            return null;
        }
        return TypesettingLayoutMode.fromCode(info.getLayoutMode()).getDescription();
    }

    private static Double resolveDisplayWidth(ProductionPiece piece) {
        return piece.getTrueWidth() == null ? piece.getWidth() : piece.getTrueWidth();
    }

    private static Double resolveDisplayHeight(ProductionPiece piece) {
        return piece.getTrueHeight() == null ? piece.getHeight() : piece.getTrueHeight();
    }

    private static Double toCentimeters(Double millimeters) {
        if (millimeters == null) {
            return null;
        }
        return millimeters / 10.0;
    }

    public ProductionPiece toProductionPiece() {
        ProductionPiece piece = new ProductionPiece();
        piece.setId(this.sourceId);
        piece.setProductionPieceId(this.id);
        piece.setOrderItemId(this.orderItemId);
        piece.setQuantity(this.quantity);
        piece.setTemplateCode(this.templateCode);
        piece.setStatus(this.status);
        piece.setIsUrgent(this.isUrgent);
        piece.setIsRedo(this.isRedo);
        piece.setCreateTime(this.createTime);
        return piece;
    }

    public TypesettingInfo toTypesettingInfo() {
        TypesettingInfo info = new TypesettingInfo();
        info.setId(this.sourceId);
        info.setTypesettingId(this.id);
        info.setQuantity(this.quantity);
        info.setLeaveQuantity(this.leaveQuantity);
        info.setMaterialConfig(this.materialConfig);
        info.setOriName(this.oriName);
        info.setOriMaterialId(this.oriMaterialId);
        info.setOriMaterialType(this.oriMaterialType);
        info.setMaterialConfigs(this.materialConfigs == null ? new ArrayList<>() : this.materialConfigs);
        info.setProcessingFlow(this.processingFlow);
        info.setStatus(this.status);
        info.setIsUrgent(this.isUrgent);
        info.setRemark(this.remark);
        info.setMaskSvg(this.maskSvg);
        info.setLayoutMode(this.layoutMode);
        info.setCreateTime(this.createTime);
        return info;
    }
}

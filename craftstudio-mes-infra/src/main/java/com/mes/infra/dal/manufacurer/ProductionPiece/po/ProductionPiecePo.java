package com.mes.infra.dal.manufacurer.ProductionPiece.po;

import com.mes.domain.delivery.deliveryRoute.vo.OrgInfo;
import com.mes.domain.manufacturer.productionPiece.entity.DeliveryPkgInfo;
import com.mes.domain.manufacturer.productionPiece.entity.Blood;
import com.mes.domain.manufacturer.productionPiece.entity.MirrorConfig;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.vo.OrderChannelInfo;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.infra.base.BasePO;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "productionPiece")
public class ProductionPiecePo extends BasePO<ProductionPiece> {

    private String productionPieceId;
    private String orderItemId;
    private OrderChannelInfo channel;
    private String procedureFlowId;
    private String carrierId;
    private String manufacturerId;
    private String routeId;
    private String routeNodeId;
    private String status;
    private Boolean isUrgent;
    private String productionPieceType;
    private MaterialConfig materialConfig;
    private Integer quantity;
    private String templateCode;
    private String positionType;
    private String positionCode;
    private ImageFile productImageFile;
    private ImageFile maskImageFile;
    /**
     * 生产工件附加标记资源。
     *
     * <p>结构与 ProductionPiece.marks / TypesettingInfo.marks 保持一致，key 表示 mark 类型，value 表示 OSS 地址。
     * 留白预处理生成的外框 PNG、四角打扣预处理使用的扣点图片资源会写入这里并持久化到 productionPiece 集合。</p>
     */
    private Map<String, String> marks;
    private String routeImg;
    private String routeSvg;
    private Double width;
    private Double height;
    private Double trueWidth;
    private Double trueHeight;
    private Blood blood;
    private String group;
    private Integer seq;
    private String processingFlow;
    private ProcedureFlow procedureFlow;
    private List<DeliveryPkgInfo> deliveryPkgInfos;
    private List<MirrorConfig> mirrorConfigs;
    private String remark;
    private OrgInfo orgInfo;

    @Override
    public ProductionPiece toDO() {
        ProductionPiece piece = new ProductionPiece();
        piece.setId(getId());
        piece.setCreateTime(getCreateTime());
        piece.setUpdateTime(getUpdateTime());

        piece.setProductionPieceId(this.productionPieceId);
        piece.setCarrierId(this.carrierId);
        piece.setOrderItemId(this.orderItemId);
        piece.setChannel(this.channel);
        piece.setProcedureFlowId(this.procedureFlowId);
        piece.setManufacturerId(this.manufacturerId);
        piece.setRouteId(this.routeId);
        piece.setRouteNodeId(this.routeNodeId);
        piece.setStatus(this.status);
        piece.setIsUrgent(this.isUrgent);
        piece.setProductionPieceType(this.productionPieceType);
        piece.setMaterialConfig(this.materialConfig);
        piece.setQuantity(this.quantity);
        piece.setTemplateCode(this.templateCode);
        piece.setPositionType(this.positionType);
        piece.setPositionCode(this.positionCode);
        piece.setRouteImg(this.routeImg);
        piece.setRouteSvg(this.routeSvg);
        piece.setProductImageFile(this.productImageFile);
        piece.setMaskImageFile(this.maskImageFile);
        piece.setMarks(this.marks);
        if (piece.getProductImageFile() != null && this.routeImg != null) {
            piece.setRouteImg(this.routeImg);
        }
        if (piece.getMaskImageFile() != null && this.routeSvg != null) {
            piece.setRouteSvg(this.routeSvg);
        }
        piece.setWidth(this.width);
        piece.setHeight(this.height);
        piece.setTrueWidth(this.trueWidth);
        piece.setTrueHeight(this.trueHeight);
        piece.setBlood(this.blood);
        piece.setGroup(this.group);
        piece.setSeq(this.seq);
        piece.setProcessingFlow(this.processingFlow);
        piece.setProcedureFlow(this.procedureFlow);
        piece.setDeliveryPkgInfos(this.deliveryPkgInfos);
        piece.setMirrorConfigs(this.mirrorConfigs);
        piece.setRemark(this.remark);
        piece.setOrgInfo(this.orgInfo);

        return piece;
    }

    @Override
    protected BasePO<ProductionPiece> fromDO(ProductionPiece _do) {
        if (_do == null) {
            return null;
        }

        this.productionPieceId = _do.getProductionPieceId();
        this.orderItemId = _do.getOrderItemId();
        this.channel = _do.getChannel();
        this.carrierId = _do.getCarrierId();
        this.procedureFlowId = _do.getProcedureFlowId();
        this.manufacturerId = _do.getManufacturerId();
        this.routeId = _do.getRouteId();
        this.routeNodeId = _do.getRouteNodeId();
        this.status = _do.getStatus();
        this.isUrgent = _do.getIsUrgent();
        this.productionPieceType = _do.getProductionPieceType();
        this.materialConfig = _do.getMaterialConfig();
        this.quantity = _do.getQuantity();
        this.templateCode = _do.getTemplateCode();
        this.positionType = _do.getPositionType();
        this.positionCode = _do.getPositionCode();
        this.routeImg = _do.getRouteImg();
        this.routeSvg = _do.getRouteSvg();
        this.productImageFile = _do.getProductImageFile();
        this.maskImageFile = _do.getMaskImageFile();
        this.marks = _do.getMarks();
        if (this.routeImg == null) {
            this.routeImg = _do.getProductImageFile() == null ? null : _do.getRouteImg();
        }
        if (this.routeSvg == null) {
            this.routeSvg = _do.getMaskImageFile() == null ? null : _do.getRouteSvg();
        }
        this.width = _do.getWidth();
        this.height = _do.getHeight();
        this.trueWidth = _do.getTrueWidth();
        this.trueHeight = _do.getTrueHeight();
        this.blood = _do.getBlood();
        this.group = _do.getGroup();
        this.seq = _do.getSeq();
        this.processingFlow = _do.getProcessingFlow();
        this.procedureFlow = _do.getProcedureFlow();
        this.deliveryPkgInfos = _do.getDeliveryPkgInfos();
        this.mirrorConfigs = _do.getMirrorConfigs();
        this.remark = _do.getRemark();
        this.orgInfo = _do.getOrgInfo();

        return this;
    }
}

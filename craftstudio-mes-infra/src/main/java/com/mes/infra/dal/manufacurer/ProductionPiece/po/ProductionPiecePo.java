package com.mes.infra.dal.manufacurer.ProductionPiece.po;

import com.mes.domain.manufacturer.productionPiece.entity.DeliveryPkgInfo;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.infra.base.BasePO;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "productionPiece")
public class ProductionPiecePo extends BasePO<ProductionPiece> {

    private String productionPieceId;
    private String orderItemId;
    private String procedureFlowId;
    private String carrierId;
    private String manufacturerId;
    private String status;
    private String productionPieceType;
    private MaterialConfig materialConfig;
    private Integer quantity;
    private String templateCode;
    private String positionType;
    private String positionCode;
    private ImageFile productImageFile;
    private ImageFile maskImageFile;
    private String processingFlow;
    private ProcedureFlow procedureFlow;
    private List<DeliveryPkgInfo> deliveryPkgInfos;

    @Override
    public ProductionPiece toDO() {
        ProductionPiece piece = new ProductionPiece();
        piece.setId(getId());
        piece.setCreateTime(getCreateTime());
        piece.setUpdateTime(getUpdateTime());

        piece.setProductionPieceId(this.productionPieceId);
        piece.setCarrierId(this.carrierId);
        piece.setOrderItemId(this.orderItemId);
        piece.setProcedureFlowId(this.procedureFlowId);
        piece.setManufacturerId(this.manufacturerId);
        piece.setStatus(this.status);
        piece.setProductionPieceType(this.productionPieceType);
        piece.setMaterialConfig(this.materialConfig);
        piece.setQuantity(this.quantity);
        piece.setTemplateCode(this.templateCode);
        piece.setPositionType(this.positionType);
        piece.setPositionCode(this.positionCode);
        piece.setProductImageFile(this.productImageFile);
        piece.setMaskImageFile(this.maskImageFile);
        piece.setProcessingFlow(this.processingFlow);
        piece.setProcedureFlow(this.procedureFlow);
        piece.setDeliveryPkgInfos(this.deliveryPkgInfos);

        return piece;
    }

    @Override
    protected BasePO<ProductionPiece> fromDO(ProductionPiece _do) {
        if (_do == null) {
            return null;
        }

        this.productionPieceId = _do.getProductionPieceId();
        this.orderItemId = _do.getOrderItemId();
        this.carrierId = _do.getCarrierId();
        this.procedureFlowId = _do.getProcedureFlowId();
        this.manufacturerId = _do.getManufacturerId();
        this.status = _do.getStatus();
        this.productionPieceType = _do.getProductionPieceType();
        this.materialConfig = _do.getMaterialConfig();
        this.quantity = _do.getQuantity();
        this.templateCode = _do.getTemplateCode();
        this.positionType = _do.getPositionType();
        this.positionCode = _do.getPositionCode();
        this.productImageFile = _do.getProductImageFile();
        this.maskImageFile = _do.getMaskImageFile();
        this.processingFlow = _do.getProcessingFlow();
        this.procedureFlow = _do.getProcedureFlow();
        this.deliveryPkgInfos = _do.getDeliveryPkgInfos();

        return this;
    }
}

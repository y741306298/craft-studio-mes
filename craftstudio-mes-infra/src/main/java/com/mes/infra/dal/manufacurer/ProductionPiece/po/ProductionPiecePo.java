package com.mes.infra.dal.manufacurer.ProductionPiece.po;

import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.FlowStatus;
import com.mes.infra.base.BasePO;
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
    private String status;
    private String productionPieceType;
    private Integer quantity;
    private String templateCode;
    private String positionType;
    private String positionCode;
    
    private String procedureFlowName;
    private String flowDescription;
    private FlowStatus flowStatus;
    private List<ProcedureFlowNodePo> nodes;
    private Integer totalNodes;

    @Override
    public ProductionPiece toDO() {
        ProductionPiece productionPiece = new ProductionPiece();
        productionPiece.setId(getId());
        productionPiece.setCreateTime(getCreateTime());
        productionPiece.setUpdateTime(getUpdateTime());
        
        productionPiece.setProductionPieceId(this.productionPieceId);
        productionPiece.setOrderItemId(this.orderItemId);
        productionPiece.setProcedureFlowId(this.procedureFlowId);
        productionPiece.setStatus(this.status);
        productionPiece.setProductionPieceType(this.productionPieceType);
        productionPiece.setQuantity(this.quantity);
        productionPiece.setTemplateCode(this.templateCode);
        productionPiece.setPositionType(this.positionType);
        productionPiece.setPositionCode(this.positionCode);
        
        if (this.procedureFlowId != null) {
            ProcedureFlow procedureFlow = new ProcedureFlow();
            procedureFlow.setId(this.procedureFlowId);
            procedureFlow.setProcedureFlowName(this.procedureFlowName);
            procedureFlow.setFlowDescription(this.flowDescription);
            procedureFlow.setFlowStatus(this.flowStatus);
            procedureFlow.setTotalNodes(this.totalNodes);
            
            if (this.nodes != null && !this.nodes.isEmpty()) {
                List<ProcedureFlowNode> nodeList = this.nodes.stream()
                    .map(ProcedureFlowNodePo::toDO)
                    .toList();
                procedureFlow.setNodes(nodeList);
            }
            
            productionPiece.setProcedureFlow(procedureFlow);
        }
        
        return productionPiece;
    }

    @Override
    protected BasePO<ProductionPiece> fromDO(ProductionPiece _do) {
        if (_do == null) {
            return null;
        }
        
        this.productionPieceId = _do.getProductionPieceId();
        this.orderItemId = _do.getOrderItemId();
        this.procedureFlowId = _do.getProcedureFlowId();
        this.status = _do.getStatus();
        this.productionPieceType = _do.getProductionPieceType();
        this.quantity = _do.getQuantity();
        this.templateCode = _do.getTemplateCode();
        this.positionType = _do.getPositionType();
        this.positionCode = _do.getPositionCode();
        
        ProcedureFlow procedureFlow = _do.getProcedureFlow();
        if (procedureFlow != null) {
            this.procedureFlowId = procedureFlow.getId() != null ? procedureFlow.getId() : this.procedureFlowId;
            this.procedureFlowName = procedureFlow.getProcedureFlowName();
            this.flowDescription = procedureFlow.getFlowDescription();
            this.flowStatus = procedureFlow.getFlowStatus();
            this.totalNodes = procedureFlow.getTotalNodes();
            
            if (procedureFlow.getNodes() != null && !procedureFlow.getNodes().isEmpty()) {
                List<ProcedureFlowNodePo> nodePos = procedureFlow.getNodes().stream()
                    .map(node -> ProcedureFlowNodePo.fromDO(node, ProcedureFlowNodePo.class))
                    .toList();
                this.nodes = nodePos;
            }
        }
        
        return this;
    }
}

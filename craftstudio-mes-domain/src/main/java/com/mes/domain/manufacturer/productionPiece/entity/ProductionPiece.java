package com.mes.domain.manufacturer.productionPiece.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.w3c.dom.Node;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProductionPiece extends BaseEntity {

    private String productionPieceId;
    private String orderItemId;
    private String manufacturerId;
    private String procedureFlowId;
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

}

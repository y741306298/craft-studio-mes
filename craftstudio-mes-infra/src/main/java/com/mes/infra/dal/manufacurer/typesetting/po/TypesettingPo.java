package com.mes.infra.dal.manufacurer.typesetting.po;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.vo.ProductionPieceCell;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingCell;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "typesetting")
public class TypesettingPo extends BasePO<TypesettingInfo> {

    private String typesettingId;
    private List<TypesettingElement> elements;
    private List<String> materialCodes;
    private String status;
    private Integer quantity;
    private Integer completedQuantity;
    private List<TypesettingCell> typesettingCells;
    private List<ProductionPieceCell> pieceCells;
    private ProcedureFlow procedureFlow;
    private String remark;

    @Override
    public TypesettingInfo toDO() {
        TypesettingInfo typesettingInfo = new TypesettingInfo();
        typesettingInfo.setId(getId());
        typesettingInfo.setCreateTime(getCreateTime());
        typesettingInfo.setUpdateTime(getUpdateTime());
        typesettingInfo.setTypesettingId(this.typesettingId);
        typesettingInfo.setElements(this.elements);
        typesettingInfo.setMaterialCodes(this.materialCodes);
        typesettingInfo.setStatus(this.status);
        typesettingInfo.setQuantity(this.quantity);
        typesettingInfo.setCompletedQuantity(this.completedQuantity);
        typesettingInfo.setTypesettingCells(this.typesettingCells);
        typesettingInfo.setPieceCells(this.pieceCells);
        typesettingInfo.setProcedureFlow(this.procedureFlow);
        typesettingInfo.setRemark(this.remark);
        return typesettingInfo;
    }

    @Override
    protected BasePO<TypesettingInfo> fromDO(TypesettingInfo _do) {
        this.typesettingId = _do.getTypesettingId();
        this.elements = _do.getElements();
        this.materialCodes = _do.getMaterialCodes();
        this.status = _do.getStatus();
        this.quantity = _do.getQuantity();
        this.completedQuantity = _do.getCompletedQuantity();
        this.typesettingCells = _do.getTypesettingCells();
        this.pieceCells = _do.getPieceCells();
        this.procedureFlow = _do.getProcedureFlow();
        this.remark = _do.getRemark();
        return this;
    }
}

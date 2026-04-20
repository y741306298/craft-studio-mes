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
    private TypesettingElement elements;
    private List<String> materialCodes;
    private String status;
    private Integer quantity;
    private Integer completedQuantity;
    private List<TypesettingCell> typesettingCells;
    private List<ProductionPieceCell> pieceCells;
    private ProcedureFlow procedureFlow;
    private String remark;
    private String maskSvg;
    private String layoutMode;
    private String layoutCategory;
    private Boolean requireJsonFile;
    private Boolean requirePltFile;
    private Boolean requireSvgFile;
    private String codeGenerateType;
    private String tempCodeFormat;
    private String anchorPointShape;

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
        typesettingInfo.setMaskSvg(this.maskSvg);
        typesettingInfo.setLayoutMode(this.layoutMode);
        typesettingInfo.setLayoutCategory(this.layoutCategory);
        typesettingInfo.setRequireJsonFile(this.requireJsonFile);
        typesettingInfo.setRequirePltFile(this.requirePltFile);
        typesettingInfo.setRequireSvgFile(this.requireSvgFile);
        typesettingInfo.setCodeGenerateType(this.codeGenerateType);
        typesettingInfo.setTempCodeFormat(this.tempCodeFormat);
        typesettingInfo.setAnchorPointShape(this.anchorPointShape);
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
        this.maskSvg = _do.getMaskSvg();
        this.layoutMode = _do.getLayoutMode();
        this.layoutCategory = _do.getLayoutCategory();
        this.requireJsonFile = _do.getRequireJsonFile();
        this.requirePltFile = _do.getRequirePltFile();
        this.requireSvgFile = _do.getRequireSvgFile();
        this.codeGenerateType = _do.getCodeGenerateType();
        this.tempCodeFormat = _do.getTempCodeFormat();
        this.anchorPointShape = _do.getAnchorPointShape();
        return this;
    }
}

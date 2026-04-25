package com.mes.infra.dal.manufacurer.typesetting.po;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "typesetting")
public class TypesettingPo extends BasePO<TypesettingInfo> {
    private String manufacturerMetaId;
    private String typesettingId;
    private TypesettingElement element;
    private MaterialConfig materialConfig;
    private List<String> materialConfigs;
    private String processingFlow;
    private String status;
    private Integer quantity;
    private Integer leaveQuantity;
    private List<TypesettingSourceCell> typesettingCells;
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
    private Map<String, String> marks;
    @Override
    public TypesettingInfo toDO() {
        TypesettingInfo typesettingInfo = new TypesettingInfo();
        typesettingInfo.setId(getId());
        typesettingInfo.setCreateTime(getCreateTime());
        typesettingInfo.setUpdateTime(getUpdateTime());
        typesettingInfo.setManufacturerMetaId(this.manufacturerMetaId);
        typesettingInfo.setTypesettingId(this.typesettingId);
        typesettingInfo.setElement(this.element);
        typesettingInfo.setMaterialConfig(this.materialConfig);
        typesettingInfo.setMaterialConfigs(this.materialConfigs);
        typesettingInfo.setProcessingFlow(this.processingFlow);
        typesettingInfo.setStatus(this.status);
        typesettingInfo.setQuantity(this.quantity);
        typesettingInfo.setLeaveQuantity(this.leaveQuantity);
        typesettingInfo.setTypesettingCells(this.typesettingCells);
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
        typesettingInfo.setMarks(this.marks);
        return typesettingInfo;
    }

    @Override
    protected BasePO<TypesettingInfo> fromDO(TypesettingInfo _do) {
        this.typesettingId = _do.getTypesettingId();
        this.element = _do.getElement();
        this.materialConfig = _do.getMaterialConfig();
        this.materialConfigs = _do.getMaterialConfigs();
        this.processingFlow = _do.getProcessingFlow();
        this.status = _do.getStatus();
        this.quantity = _do.getQuantity();
        this.leaveQuantity = _do.getLeaveQuantity();
        this.manufacturerMetaId = _do.getManufacturerMetaId();
        this.typesettingCells = _do.getTypesettingCells();
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
        this.marks = _do.getMarks();
        return this;
    }
}

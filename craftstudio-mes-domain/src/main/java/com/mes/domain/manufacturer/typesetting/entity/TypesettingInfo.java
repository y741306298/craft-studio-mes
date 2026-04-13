package com.mes.domain.manufacturer.typesetting.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.vo.ProductionPieceCell;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingCell;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class TypesettingInfo extends BaseEntity {
    //排版文件
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

}

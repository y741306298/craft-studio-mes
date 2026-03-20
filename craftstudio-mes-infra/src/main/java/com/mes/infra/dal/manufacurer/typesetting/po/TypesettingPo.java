package com.mes.infra.dal.manufacurer.typesetting.po;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.vo.OrderItemCell;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingCell;
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
    private String typesettingUrl;
    private String material;
    private TypesettingStatus status;
    private Integer quantity;
    private Integer completedQuantity;
    private List<TypesettingCell> typesettingCells;
    private List<OrderItemCell> orderItemCells;
    private ProcedureFlow procedureFlow;

    @Override
    public TypesettingInfo toDO() {
        TypesettingInfo typesettingInfo = new TypesettingInfo();
        typesettingInfo.setId(getId());
        typesettingInfo.setCreateTime(getCreateTime());
        typesettingInfo.setUpdateTime(getUpdateTime());
        typesettingInfo.setTypesettingId(this.typesettingId);
        typesettingInfo.setTypesettingUrl(this.typesettingUrl);
        typesettingInfo.setMaterial(this.material);
        typesettingInfo.setStatus(this.status);
        typesettingInfo.setQuantity(this.quantity);
        typesettingInfo.setCompletedQuantity(this.completedQuantity);
        typesettingInfo.setTypesettingCells(this.typesettingCells);
        typesettingInfo.setOrderItemCells(this.orderItemCells);
        typesettingInfo.setProcedureFlow(this.procedureFlow);
        return typesettingInfo;
    }

    @Override
    protected BasePO<TypesettingInfo> fromDO(TypesettingInfo _do) {
        this.typesettingId = _do.getTypesettingId();
        this.typesettingUrl = _do.getTypesettingUrl();
        this.material = _do.getMaterial();
        this.status = _do.getStatus();
        this.quantity = _do.getQuantity();
        this.completedQuantity = _do.getCompletedQuantity();
        this.typesettingCells = _do.getTypesettingCells();
        this.orderItemCells = _do.getOrderItemCells();
        this.procedureFlow = _do.getProcedureFlow();
        return this;
    }
}

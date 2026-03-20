package com.mes.domain.order.OrderItemProcedureCfg.entity;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderItemProcedureCfg extends BaseEntity {

    private String orderItemId;
    private String procedureId;
    private String productionPieceId;
    private String procedureName;
    private Integer productionPieceQuantity;

}

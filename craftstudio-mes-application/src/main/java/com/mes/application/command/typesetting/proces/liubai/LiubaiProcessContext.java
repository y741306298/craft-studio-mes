package com.mes.application.command.typesetting.proces.liubai;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import lombok.Data;

@Data
public class LiubaiProcessContext {
    private OrderItem orderItem;
    private ProcedureFlow procedureFlow;
    private ProductionPiece productionPiece;
    private boolean skipBloodEdges;
}

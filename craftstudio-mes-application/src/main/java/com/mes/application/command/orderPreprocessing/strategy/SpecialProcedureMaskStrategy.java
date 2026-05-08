package com.mes.application.command.orderPreprocessing.strategy;

import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpecialProcedureMaskStrategy implements OrderItemProcessingStrategy {

    @Override
    public boolean matches(OrderItem orderItem, ProcedureFlow procedureFlow) {
        return AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "超幅拼接")
                || AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "异形切割");
    }

    @Override
    public List<ProductionPiece> process(OrderItem orderItem, ProcedureFlow procedureFlow, AppOrderPreprocessingService processingService) {
        return processingService.processWithCuttingOrSpecialShape(orderItem, procedureFlow, getStrategyType());
    }

    @Override
    public String getStrategyType() {
        return "SPECIAL_PROCEDURE";
    }

    @Override
    public String getStrategyRemark() {
        return "存在超幅拼接或异形切割工艺，走裁切/蒙版异步处理";
    }
}

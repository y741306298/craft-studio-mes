package com.mes.application.command.orderPreprocessing.strategy;

import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoSpecialProcedureStrategy implements OrderItemProcessingStrategy {

    @Override
    public boolean matches(OrderItem orderItem, ProcedureFlow procedureFlow) {
        return !AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "超幅拼接")
                && !AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "异形切割");
    }

    @Override
    public List<ProductionPiece> process(OrderItem orderItem, ProcedureFlow procedureFlow, AppOrderPreprocessingService processingService) {
        return processingService.processWithoutCuttingAndMasking(orderItem, procedureFlow);
    }

    @Override
    public String getStrategyType() {
        return "NONE";
    }

    @Override
    public String getStrategyRemark() {
        return "无特殊工艺，直接生成生产零件";
    }
}

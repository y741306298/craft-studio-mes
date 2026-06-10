package com.mes.application.command.orderPreprocessing.strategy;

import com.mes.application.command.orderPreprocessing.AppOrderPreprocessingService;
import com.mes.application.command.orderPreprocessing.splice.SpliceProcessStrategies;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.util.ProcedureFlowNodeMatcher;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpecialProcedureMaskStrategy implements OrderItemProcessingStrategy {

    @Override
    public boolean matches(OrderItem orderItem, ProcedureFlow procedureFlow) {
        boolean hasSplicingOrSpecialShape = SpliceProcessStrategies.hasSpliceNode(procedureFlow)
                || AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "异形切割");
        boolean hasDoubleSide = ProcedureFlowNodeMatcher.hasDoubleSideMountingNode(procedureFlow);
        return hasSplicingOrSpecialShape && !hasDoubleSide;
    }

    @Override
    public List<ProductionPiece> process(OrderItem orderItem, ProcedureFlow procedureFlow, AppOrderPreprocessingService processingService) {
        // 步骤1：识别拼接/异形切割组合。
        boolean hasSplicing = SpliceProcessStrategies.hasSpliceNode(procedureFlow);
        boolean hasSpecialShape = AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "异形切割");

        if (hasSplicing && !hasSpecialShape) {
            // 步骤2：仅拼接时先生成等幅 SVG 蒙版并保存。
            String generatedMaskImgUrl = processingService.generateRectMaskSvgForStrategy(orderItem);
            processingService.saveMaskToOrderItemForStrategy(orderItem, generatedMaskImgUrl);
        }

        // 步骤3：异步触发蒙版算法（无 mirrorUrl）。
        processingService.callMaskAsyncForStrategy(orderItem, procedureFlow, getStrategyType(), hasSpecialShape, hasSplicing, null);
        return null;
    }

    @Override
    public String getStrategyType() {
        return "SPECIAL_PROCEDURE";
    }

    @Override
    public String getStrategyRemark() {
        return "存在拼接或异形切割工艺，走裁切/蒙版异步处理";
    }
}

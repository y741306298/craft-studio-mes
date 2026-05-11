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
        boolean hasCuttingOrSpecialShape = AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "超幅拼接")
                || AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "异形切割");
        boolean hasDoubleSide = AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "双面对裱")
                || AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "覆双面");
        return hasCuttingOrSpecialShape && !hasDoubleSide;
    }

    @Override
    public List<ProductionPiece> process(OrderItem orderItem, ProcedureFlow procedureFlow, AppOrderPreprocessingService processingService) {
        // 步骤1：识别超幅拼接/异形切割组合。
        boolean hasCutting = AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "超幅拼接");
        boolean hasSpecialShape = AppOrderPreprocessingService.hasNodeWithName(procedureFlow, "异形切割");

        if (hasCutting && !hasSpecialShape) {
            // 步骤2：仅超幅拼接时先生成等幅 SVG 蒙版并保存。
            String generatedMaskImgUrl = processingService.generateRectMaskSvgForStrategy(orderItem);
            processingService.saveMaskToOrderItemForStrategy(orderItem, generatedMaskImgUrl);
        }

        // 步骤3：异步触发蒙版算法（无 mirrorUrl）。
        processingService.callMaskAsyncForStrategy(orderItem, procedureFlow, getStrategyType(), hasSpecialShape, hasCutting, null);
        return null;
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

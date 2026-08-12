package com.mes.application.command.orderPreprocessing;

import com.mes.application.command.typesetting.proces.buckle.FourCornerBuckleProcessService;
import com.mes.application.command.typesetting.proces.liubai.LiubaiProcessService;
import com.mes.application.command.typesetting.proces.splice.SuperWidthSpliceProcessService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AppOrderPreprocessingServiceMarklessSpliceTest {

    @Test
    void shouldRecognizeSpliceFlowsThatMustNotGenerateMarks() {
        for (String nodeName : List.of("写真拼接", "无痕拼接", "板材拼接")) {
            assertTrue(AppOrderPreprocessingService.isMarklessSpliceFlow(flow(nodeName)));
        }
        assertFalse(AppOrderPreprocessingService.isMarklessSpliceFlow(flow("超幅拼接")));
    }

    @Test
    void shouldSkipAllMarkServicesForMarklessSpliceFlows() {
        AppOrderPreprocessingService service = new AppOrderPreprocessingService();
        SuperWidthSpliceProcessService spliceService = mock(SuperWidthSpliceProcessService.class);
        LiubaiProcessService liubaiService = mock(LiubaiProcessService.class);
        FourCornerBuckleProcessService buckleService = mock(FourCornerBuckleProcessService.class);
        ReflectionTestUtils.setField(service, "superWidthSpliceProcessService", spliceService);
        ReflectionTestUtils.setField(service, "liubaiProcessService", liubaiService);
        ReflectionTestUtils.setField(service, "fourCornerBuckleProcessService", buckleService);

        OrderItem orderItem = new OrderItem();
        ProductionPiece piece = new ProductionPiece();
        piece.setSeq(1);
        piece.setGroup("group-1");
        ProcedureFlow procedureFlow = flow("写真拼接");

        service.applySpliceProcessForStrategy(orderItem, procedureFlow, piece, null);
        service.applyBuckleAndLiubaiProcessForStrategy(orderItem, procedureFlow, piece, true);

        verifyNoInteractions(spliceService, liubaiService, buckleService);
    }

    private ProcedureFlow flow(String nodeName) {
        ProcedureFlowNode node = new ProcedureFlowNode();
        node.setNodeName(nodeName);
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(List.of(node));
        return flow;
    }
}

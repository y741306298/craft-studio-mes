package com.mes.application.command.order;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppOrderServiceTest {

    private final AppOrderService service = new AppOrderService();

    @Test
    void rejectsTransferQuantityEqualToPendingTypesettingQuantity() {
        assertThat(hasPendingTypesettingQuantityAtMost(productionPiece(5), 5)).isTrue();
    }

    @Test
    void rejectsTransferQuantityGreaterThanPendingTypesettingQuantity() {
        assertThat(hasPendingTypesettingQuantityAtMost(productionPiece(5), 6)).isTrue();
    }

    @Test
    void allowsTransferQuantityLessThanPendingTypesettingQuantity() {
        assertThat(hasPendingTypesettingQuantityAtMost(productionPiece(5), 4)).isFalse();
    }

    private boolean hasPendingTypesettingQuantityAtMost(ProductionPiece piece, int transferQuantity) {
        return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(
                service,
                "hasPendingTypesettingQuantityAtMost",
                piece,
                transferQuantity
        ));
    }

    private ProductionPiece productionPiece(int pendingQuantity) {
        ProcedureFlowNode pendingTypesetting = new ProcedureFlowNode();
        pendingTypesetting.setNodeName("待排版");
        pendingTypesetting.setPieceQuantity(pendingQuantity);
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(List.of(pendingTypesetting));
        ProductionPiece piece = new ProductionPiece();
        piece.setProcedureFlow(flow);
        return piece;
    }
}

package com.mes.application.command.delivery;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppDeliveryPkgServiceTest {

    @Test
    void fullyPackedTransitionConsolidatesQuantityAtPackedNode() {
        ProcedureFlowNode printing = node("NODE_PRINTING_IN_PROGRESS", "打印中", 2);
        ProcedureFlowNode pendingPacking = node("NODE_PENDING_PACKING", "待打包", 3);
        ProcedureFlowNode packed = node("NODE_PACKAGED", "已打包", 10);
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(List.of(printing, pendingPacking, packed));
        ProductionPiece piece = new ProductionPiece();
        piece.setQuantity(10);
        piece.setStatus(TypesettingStatus.PRINTING_IN_PROGRESS.getCode());
        piece.setProcedureFlow(flow);

        ReflectionTestUtils.invokeMethod(new AppDeliveryPkgService(),
                "updatePiecePackagingStateAfterTransfer", piece, new HashSet<String>());

        assertThat(piece.getStatus()).isEqualTo(TypesettingStatus.COMPLETED.getCode());
        assertThat(printing.getPieceQuantity()).isZero();
        assertThat(pendingPacking.getPieceQuantity()).isZero();
        assertThat(packed.getPieceQuantity()).isEqualTo(10);
    }

    private ProcedureFlowNode node(String id, String name, int quantity) {
        ProcedureFlowNode node = new ProcedureFlowNode();
        node.setNodeId(id);
        node.setNodeName(name);
        node.setPieceQuantity(quantity);
        return node;
    }
}

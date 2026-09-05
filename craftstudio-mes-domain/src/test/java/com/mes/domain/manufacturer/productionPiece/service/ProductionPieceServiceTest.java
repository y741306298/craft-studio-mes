package com.mes.domain.manufacturer.productionPiece.service;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.PieceQuantityTransfer;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.repository.ProductionPieceRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductionPieceServiceTest {

    @Test
    void strictTransferRejectsBatchWhenPendingQuantityIsInsufficient() {
        ProductionPieceRepository repository = mock(ProductionPieceRepository.class);
        ProductionPieceService service = new ProductionPieceService();
        ReflectionTestUtils.setField(service, "productionPieceRepository", repository);
        ProductionPiece piece = piece("piece-1", 2, 0, 5);
        when(repository.findByProductionPieceIds(List.of("piece-1"))).thenReturn(List.of(piece));

        assertThatThrownBy(() -> service.transferPieceQuantitiesBetweenNodesStrict(List.of(
                new PieceQuantityTransfer("piece-1", "NODE_TYPESETTING",
                        "NODE_TYPESETTING_IN_PROGRESS", 3))))
                .isInstanceOf(BusinessNotAllowException.class)
                .hasMessageContaining("数量不足")
                .hasMessageContaining("需求=3")
                .hasMessageContaining("可用=2");

        verify(repository, never()).batchUpdate(anyList());
    }

    private ProductionPiece piece(String id, int pendingQuantity, int inProgressQuantity, int totalQuantity) {
        ProcedureFlowNode pending = new ProcedureFlowNode();
        pending.setNodeId("NODE_TYPESETTING");
        pending.setNodeName("待排版");
        pending.setPieceQuantity(pendingQuantity);
        ProcedureFlowNode inProgress = new ProcedureFlowNode();
        inProgress.setNodeId("NODE_TYPESETTING_IN_PROGRESS");
        inProgress.setNodeName("排版中");
        inProgress.setPieceQuantity(inProgressQuantity);
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(List.of(pending, inProgress));
        ProductionPiece piece = new ProductionPiece();
        piece.setId(id);
        piece.setProductionPieceId(id);
        piece.setQuantity(totalQuantity);
        piece.setProcedureFlow(flow);
        return piece;
    }
}

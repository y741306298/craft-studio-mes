package com.mes.domain.manufacturer.productionPiece.service;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.PieceQuantityTransfer;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.repository.PieceQuantityDeficitRecordRepository;
import com.mes.domain.manufacturer.productionPiece.repository.ProductionPieceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductionPieceServiceTest {

    @Test
    void transferCapsTargetAdditionAtProductionPieceQuantity() {
        ProductionPieceRepository repository = mock(ProductionPieceRepository.class);
        PieceQuantityDeficitRecordRepository deficitRepository = mock(PieceQuantityDeficitRecordRepository.class);
        ProductionPieceService service = new ProductionPieceService();
        ReflectionTestUtils.setField(service, "productionPieceRepository", repository);
        ReflectionTestUtils.setField(service, "pieceQuantityDeficitRecordRepository", deficitRepository);

        ProcedureFlowNode source = node("FROM", 1);
        ProcedureFlowNode target = node("TO", 3);
        ProcedureFlowNode other = node("OTHER", 4);
        ProductionPiece piece = piece(10, source, target, other);
        when(repository.findByProductionPieceIds(anyCollection())).thenReturn(List.of(piece));

        service.transferPieceQuantitiesBetweenNodes(
                List.of(new PieceQuantityTransfer("piece-1", "FROM", "TO", 5)));

        assertThat(source.getPieceQuantity()).isZero();
        assertThat(target.getPieceQuantity()).isEqualTo(6);
        int nodeTotal = piece.getProcedureFlow().getNodes().stream()
                .mapToInt(ProcedureFlowNode::getPieceQuantity)
                .sum();
        assertThat(nodeTotal).isEqualTo(10);
        verify(repository).batchUpdate(List.of(piece));
    }

    @Test
    void transferKeepsRequestedQuantityWhenNodeTotalDoesNotGrow() {
        ProductionPieceRepository repository = mock(ProductionPieceRepository.class);
        ProductionPieceService service = new ProductionPieceService();
        ReflectionTestUtils.setField(service, "productionPieceRepository", repository);
        ReflectionTestUtils.setField(service, "pieceQuantityDeficitRecordRepository",
                mock(PieceQuantityDeficitRecordRepository.class));

        ProcedureFlowNode source = node("FROM", 5);
        ProcedureFlowNode target = node("TO", 2);
        ProductionPiece piece = piece(10, source, target, node("OTHER", 3));
        when(repository.findByProductionPieceIds(anyCollection())).thenReturn(List.of(piece));

        service.transferPieceQuantitiesBetweenNodes(
                List.of(new PieceQuantityTransfer("piece-1", "FROM", "TO", 4)));

        assertThat(source.getPieceQuantity()).isEqualTo(1);
        assertThat(target.getPieceQuantity()).isEqualTo(6);
    }

    private ProductionPiece piece(int quantity, ProcedureFlowNode... nodes) {
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(List.of(nodes));
        ProductionPiece piece = new ProductionPiece();
        piece.setId("record-1");
        piece.setProductionPieceId("piece-1");
        piece.setQuantity(quantity);
        piece.setProcedureFlow(flow);
        return piece;
    }

    private ProcedureFlowNode node(String id, int quantity) {
        ProcedureFlowNode node = new ProcedureFlowNode();
        node.setNodeId(id);
        node.setPieceQuantity(quantity);
        return node;
    }
}

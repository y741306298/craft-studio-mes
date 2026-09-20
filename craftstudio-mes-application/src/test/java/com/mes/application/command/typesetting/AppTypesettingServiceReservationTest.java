package com.mes.application.command.typesetting;

import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppTypesettingServiceReservationTest {

    @Test
    void pendingLayoutWithNoLeaveQuantityStillReservesItsSourcePieces() {
        AppTypesettingService service = new AppTypesettingService();
        TypesettingService typesettingService = mock(TypesettingService.class);
        ReflectionTestUtils.setField(service, "domainTypesettingService", typesettingService);
        ProductionPiece piece = piece("piece-1", "PP-1", 1);
        piece.setProcedureFlow(flow(node("NODE_TYPESETTING", "待排版", 1),
                node("NODE_TYPESETTING_IN_PROGRESS", "排版中", 0)));
        TypesettingInfo firstLayout = layout("LAYOUT-FIRST", "piece-1", 1);
        firstLayout.setStatus(TypesettingStatus.PENDING.getCode());
        firstLayout.setLeaveQuantity(0);
        when(typesettingService.findByProductionPieceSourceIds(anyCollection()))
                .thenReturn(List.of(firstLayout));

        String error = ReflectionTestUtils.invokeMethod(service,
                "validateExistingProductionPieceReservations", List.of(piece), Map.of("piece-1", 1));

        assertThat(error).contains("PP-1已被排版文件占用", "可用数量=0", "LAYOUT-FIRST");
    }

    private ProcedureFlow flow(ProcedureFlowNode... nodes) {
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(List.of(nodes));
        return flow;
    }

    private ProcedureFlowNode node(String id, String name, int quantity) {
        ProcedureFlowNode node = new ProcedureFlowNode();
        node.setNodeId(id);
        node.setNodeName(name);
        node.setPieceQuantity(quantity);
        return node;
    }

    private ProductionPiece piece(String id, String businessId, int requestedQuantity) {
        ProductionPiece piece = new ProductionPiece();
        piece.setId(id);
        piece.setProductionPieceId(businessId);
        piece.setQuantity(requestedQuantity);
        return piece;
    }

    private TypesettingInfo layout(String typesettingId, String sourceId, int quantity) {
        TypesettingSourceCell cell = new TypesettingSourceCell();
        cell.setSourceType(TypesettingSourceType.PART.getCode());
        cell.setSourceId(sourceId);
        cell.setQuantity(quantity);
        TypesettingInfo layout = new TypesettingInfo();
        layout.setTypesettingId(typesettingId);
        layout.setTypesettingCells(List.of(cell));
        return layout;
    }
}

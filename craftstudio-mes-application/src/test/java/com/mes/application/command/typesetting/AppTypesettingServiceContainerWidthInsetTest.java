package com.mes.application.command.typesetting;

import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.vo.TypesettingProductionPieceVO;
import com.mes.application.dto.req.typesetting.LayoutConfirmRequest;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppTypesettingServiceContainerWidthInsetTest {

    private final AppTypesettingService service = new AppTypesettingService();

    @Test
    void applyToLayoutContainerWidthInsetSubtracts16WhenAllCellsAreProductionPiecesWithCoverBoard() {
        LayoutConfirmRequest request = buildRequest(partCell("覆板"));

        service.applyToLayoutContainerWidthInset(request);

        assertThat(request.getContainers().get(0).getWidth()).isEqualTo(984);
    }

    @Test
    void applyToLayoutContainerWidthInsetSubtracts28WhenAllCellsAreProductionPiecesWithoutCoverBoard() {
        LayoutConfirmRequest request = buildRequest(partCell("排版"));

        service.applyToLayoutContainerWidthInset(request);

        assertThat(request.getContainers().get(0).getWidth()).isEqualTo(972);
    }

    @Test
    void applyToLayoutContainerWidthInsetSubtracts28WhenCellsContainTypesettingForme() {
        LayoutConfirmRequest request = buildRequest(partCell("覆板"), typesettingCell("覆板"));

        service.applyToLayoutContainerWidthInset(request);

        assertThat(request.getContainers().get(0).getWidth()).isEqualTo(972);
    }

    private LayoutConfirmRequest buildRequest(TypesettingProductionPieceVO... cells) {
        LayoutConfirmRequest request = new LayoutConfirmRequest();
        request.setTypesettingCells(new ArrayList<>(List.of(cells)));
        request.setContainers(new ArrayList<>(List.of(new LayoutConfirmRequest.ContainerInfo(1000, 1000))));
        return request;
    }

    private TypesettingProductionPieceVO partCell(String... nodeNames) {
        TypesettingProductionPieceVO cell = new TypesettingProductionPieceVO();
        cell.setSourceType(TypesettingSourceType.PART.getCode());
        cell.setProcedureFlow(procedureFlow(nodeNames));
        return cell;
    }

    private TypesettingProductionPieceVO typesettingCell(String... nodeNames) {
        TypesettingProductionPieceVO cell = new TypesettingProductionPieceVO();
        cell.setSourceType(TypesettingSourceType.TYPESETTING.getCode());
        cell.setProcedureFlow(procedureFlow(nodeNames));
        return cell;
    }

    private ProcedureFlow procedureFlow(String... nodeNames) {
        ProcedureFlow flow = new ProcedureFlow();
        List<ProcedureFlowNode> nodes = new ArrayList<>();
        for (String nodeName : nodeNames) {
            ProcedureFlowNode node = new ProcedureFlowNode();
            node.setNodeName(nodeName);
            nodes.add(node);
        }
        flow.setNodes(nodes);
        return flow;
    }
}

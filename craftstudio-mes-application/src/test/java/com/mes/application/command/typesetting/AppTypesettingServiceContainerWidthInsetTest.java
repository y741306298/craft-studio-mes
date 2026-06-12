package com.mes.application.command.typesetting;

import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.vo.TypesettingProductionPieceVO;
import com.mes.application.dto.req.typesetting.LayoutConfirmRequest;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingContainerWidthInset;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.typesetting.service.TypesettingContainerWidthInsetService;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AppTypesettingServiceContainerWidthInsetTest {

    private final AppTypesettingService service = new AppTypesettingService();

    @Test
    void applyToLayoutContainerWidthInsetSubtracts16WhenAllCellsAreProductionPiecesWithCoverBoard() {
        LayoutConfirmRequest request = buildRequest(partCell("覆板"));

        service.applyToLayoutContainerWidthInset(request);

        assertThat(request.getContainers().get(0).getWidth()).isEqualTo(984);
    }

    @Test
    void applyToLayoutContainerWidthInsetKeeps16ForCoverBoardPartsEvenWhenMaterialInsetIsConfigured() {
        TypesettingContainerWidthInsetService insetService = mock(TypesettingContainerWidthInsetService.class);
        ReflectionTestUtils.setField(service, "containerWidthInsetService", insetService);
        LayoutConfirmRequest request = buildRequest(partCellWithMaterial("material-1", "覆板"));
        request.setLayoutMode(TypesettingLayoutMode.GRID_TYPESETTING_BASIC.getCode());

        service.applyToLayoutContainerWidthInset(request);

        assertThat(request.getContainers().get(0).getWidth()).isEqualTo(984);
        verifyNoInteractions(insetService);
    }

    @Test
    void applyToLayoutContainerWidthInsetUsesConfiguredInsetForNonCoverBoardParts() {
        TypesettingContainerWidthInsetService insetService = mock(TypesettingContainerWidthInsetService.class);
        TypesettingContainerWidthInset inset = new TypesettingContainerWidthInset();
        inset.setWidthInset(40);
        when(insetService.findByMaterialIdAndLayoutMode("material-1", TypesettingLayoutMode.GRID_TYPESETTING_BASIC.getCode()))
                .thenReturn(inset);
        ReflectionTestUtils.setField(service, "containerWidthInsetService", insetService);
        LayoutConfirmRequest request = buildRequest(partCellWithMaterial("material-1", "排版"));
        request.setLayoutMode(TypesettingLayoutMode.GRID_TYPESETTING_BASIC.getCode());

        service.applyToLayoutContainerWidthInset(request);

        assertThat(request.getContainers().get(0).getWidth()).isEqualTo(960);
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

    private TypesettingProductionPieceVO partCellWithMaterial(String materialId, String... nodeNames) {
        TypesettingProductionPieceVO cell = partCell(nodeNames);
        MaterialConfig materialConfig = new MaterialConfig();
        materialConfig.setMaterialId(materialId);
        cell.setMaterialConfig(materialConfig);
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

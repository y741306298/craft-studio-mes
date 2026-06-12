package com.mes.application.command.typesetting;

import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.vo.TypesettingProductionPieceVO;
import com.mes.application.dto.req.typesetting.LayoutConfirmRequest;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingContainerWidthInset;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.typesetting.service.TypesettingContainerWidthInsetService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
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

    @Test
    void validateCellSizeAgainstContainersKeepsOnlyContainersThatFitAllCells() {
        LayoutConfirmRequest request = new LayoutConfirmRequest();
        request.setContainers(new ArrayList<>(List.of(
                new LayoutConfirmRequest.ContainerInfo(900, 1200),
                new LayoutConfirmRequest.ContainerInfo(1000, 1500),
                new LayoutConfirmRequest.ContainerInfo(800, 2000)
        )));
        ProductionPiece piece = productionPiece("piece-1", 1000D, 1400D);

        ReflectionTestUtils.invokeMethod(service, "validateCellSizeAgainstContainers",
                request, List.of(piece), List.of(), TypesettingLayoutMode.GRID_TYPESETTING_BASIC);

        assertThat(request.getContainers())
                .extracting(LayoutConfirmRequest.ContainerInfo::getWidth, LayoutConfirmRequest.ContainerInfo::getHeight)
                .containsExactly(tuple(1000, 1500));
    }

    @Test
    void validateCellSizeAgainstContainersThrowsOnlyWhenNoContainerFits() {
        LayoutConfirmRequest request = new LayoutConfirmRequest();
        request.setContainers(new ArrayList<>(List.of(
                new LayoutConfirmRequest.ContainerInfo(900, 1200),
                new LayoutConfirmRequest.ContainerInfo(800, 2000)
        )));
        ProductionPiece piece = productionPiece("piece-2", 1000D, 1400D);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "validateCellSizeAgainstContainers",
                request, List.of(piece), List.of(), TypesettingLayoutMode.GRID_TYPESETTING_BASIC))
                .hasMessageContaining("piece-2零件的尺寸大于所选规格，不能排版");
    }

    @Test
    void validateCellSizeAgainstContainersFiltersByTypesettingInfoSize() {
        LayoutConfirmRequest request = new LayoutConfirmRequest();
        request.setContainers(new ArrayList<>(List.of(
                new LayoutConfirmRequest.ContainerInfo(900, 1200),
                new LayoutConfirmRequest.ContainerInfo(1000, 1500)
        )));
        TypesettingInfo info = typesettingInfo("typesetting-1", 950, 1300);

        ReflectionTestUtils.invokeMethod(service, "validateCellSizeAgainstContainers",
                request, List.of(), List.of(info), TypesettingLayoutMode.GRID_TYPESETTING_BASIC);

        assertThat(request.getContainers())
                .extracting(LayoutConfirmRequest.ContainerInfo::getWidth, LayoutConfirmRequest.ContainerInfo::getHeight)
                .containsExactly(tuple(1000, 1500));
    }

    private ProductionPiece productionPiece(String productionPieceId, Double width, Double height) {
        ProductionPiece piece = new ProductionPiece();
        piece.setProductionPieceId(productionPieceId);
        piece.setWidth(width);
        piece.setHeight(height);
        return piece;
    }

    private TypesettingInfo typesettingInfo(String typesettingId, int width, int height) {
        TypesettingInfo info = new TypesettingInfo();
        info.setTypesettingId(typesettingId);
        TypesettingElement element = new TypesettingElement();
        element.setWidth(BigDecimal.valueOf(width));
        element.setHeight(BigDecimal.valueOf(height));
        info.setElement(element);
        return info;
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

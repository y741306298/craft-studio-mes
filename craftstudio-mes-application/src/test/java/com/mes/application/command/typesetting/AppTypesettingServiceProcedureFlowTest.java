package com.mes.application.command.typesetting;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppTypesettingServiceProcedureFlowTest {

    private final AppTypesettingService service = new AppTypesettingService();

    @Test
    void retainsFilmAndCoverBoardNodesAfterCommonPrefixEnds() {
        ProcedureFlow result = buildCommonFlow(List.of(
                piece("待排版", "工件A处理", "覆膜", "覆板", "完成"),
                piece("待排版", "工件B处理", "覆膜", "覆板", "完成")));

        assertThat(result.getNodes())
                .extracting(ProcedureFlowNode::getNodeName)
                .containsExactly("待排版", "覆膜", "覆板");
    }

    @Test
    void doesNotAddFilmWhenNotEveryProductionPieceHasIt() {
        ProcedureFlow result = buildCommonFlow(List.of(
                piece("待排版", "工件A处理", "覆膜", "覆板"),
                piece("待排版", "工件B处理", "覆板")));

        assertThat(result.getNodes())
                .extracting(ProcedureFlowNode::getNodeName)
                .containsExactly("待排版", "覆板");
    }

    @Test
    void retainsNoFilmNodeWhenEverySourceHasIt() {
        ProcedureFlow result = buildCommonFlow(
                List.of(piece("待排版", "工件处理", "不覆膜", "覆板")),
                List.of(typesettingInfo("待排版", "印版处理", "不覆膜", "覆板")));

        assertThat(result.getNodes())
                .extracting(ProcedureFlowNode::getNodeName)
                .containsExactly("待排版", "不覆膜", "覆板");
    }

    @Test
    void doesNotAddNoFilmWhenAPlateSourceDoesNotHaveIt() {
        ProcedureFlow result = buildCommonFlow(
                List.of(piece("待排版", "工件处理", "不覆膜", "覆板")),
                List.of(typesettingInfo("待排版", "印版处理", "覆板")));

        assertThat(result.getNodes())
                .extracting(ProcedureFlowNode::getNodeName)
                .containsExactly("待排版", "覆板");
    }

    private ProcedureFlow buildCommonFlow(List<ProductionPiece> pieces) {
        return buildCommonFlow(pieces, Collections.emptyList());
    }

    private ProcedureFlow buildCommonFlow(List<ProductionPiece> pieces, List<TypesettingInfo> typesettingInfos) {
        return ReflectionTestUtils.invokeMethod(
                service, "validateAndBuildCommonProcedureFlow", pieces, typesettingInfos);
    }

    private ProductionPiece piece(String... nodeNames) {
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(java.util.Arrays.stream(nodeNames).map(this::node).toList());
        ProductionPiece piece = new ProductionPiece();
        piece.setProcedureFlow(flow);
        return piece;
    }

    private ProcedureFlowNode node(String nodeName) {
        ProcedureFlowNode node = new ProcedureFlowNode();
        node.setNodeName(nodeName);
        return node;
    }

    private TypesettingInfo typesettingInfo(String... nodeNames) {
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(java.util.Arrays.stream(nodeNames).map(this::node).toList());
        TypesettingInfo info = new TypesettingInfo();
        info.setProcedureFlow(flow);
        return info;
    }
}

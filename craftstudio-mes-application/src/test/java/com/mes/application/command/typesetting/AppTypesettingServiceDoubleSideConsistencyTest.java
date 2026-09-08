package com.mes.application.command.typesetting;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppTypesettingServiceDoubleSideConsistencyTest {

    private final AppTypesettingService service = new AppTypesettingService();

    @ParameterizedTest
    @ValueSource(strings = {"双面对裱", "覆双面", "双面喷"})
    void rejectsMixingDoubleSidePieceWithOrdinaryPiece(String nodeName) {
        String result = validate(
                List.of(piece("double-side-piece", nodeName), piece("ordinary-piece", "裁切")),
                Collections.emptyList());

        assertThat(result).isEqualTo("ordinary-piece不包含双面对裱、覆双面、双面喷中的任一工艺，不能一起排版");
    }

    @Test
    void acceptsPiecesUsingDifferentDoubleSideKeywords() {
        String result = validate(
                List.of(
                        piece("mounting-piece", "PVC双面对裱"),
                        piece("cover-piece", "覆双面"),
                        piece("printing-piece", "UV双面喷")),
                Collections.emptyList());

        assertThat(result).isEqualTo("PASS");
    }

    @Test
    void doesNotTreatDoubleSideTapeAsDoubleSideMounting() {
        String result = validate(
                List.of(piece("tape-piece", "反面覆双面胶"), piece("ordinary-piece", "裁切")),
                Collections.emptyList());

        assertThat(result).isEqualTo("PASS");
    }

    @Test
    void skipsConsistencyRestrictionWhenLayoutContainsPrintingPlate() {
        String result = validate(
                List.of(piece("double-side-piece", "覆双面"), piece("ordinary-piece", "裁切")),
                List.of(new TypesettingInfo()));

        assertThat(result).isEqualTo("PASS");
    }

    private String validate(List<ProductionPiece> pieces, List<TypesettingInfo> typesettingInfos) {
        return ReflectionTestUtils.invokeMethod(
                service, "validateDoubleSideMountingConsistency", pieces, typesettingInfos);
    }

    private ProductionPiece piece(String productionPieceId, String nodeName) {
        ProcedureFlowNode node = new ProcedureFlowNode();
        node.setNodeName(nodeName);
        ProcedureFlow flow = new ProcedureFlow();
        flow.setNodes(List.of(node));
        ProductionPiece piece = new ProductionPiece();
        piece.setProductionPieceId(productionPieceId);
        piece.setProcedureFlow(flow);
        return piece;
    }
}

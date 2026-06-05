package com.mes.application.command.typesetting.proces.buckle;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.Blood;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AroundBuckleProcessStrategyTest {
    private final AroundBuckleProcessStrategy strategy = new AroundBuckleProcessStrategy(null, null);

    @Test
    void superWidthSpliceUsesBloodAndCoveredEdgesForFifteenCentimeterOffset() {
        ProductionPiece piece = new ProductionPiece();
        piece.setSeq(1);
        piece.setGroup("# 1-3");
        Blood blood = new Blood();
        blood.setX(-1);
        blood.setY(1);
        piece.setBlood(blood);

        List<BuckleMarkPoint> points = strategy.buildMarkPoints(context(piece, "超幅拼接"), 1000D, 500D);

        BuckleMarkPoint lt = find(points, "lt");
        BuckleMarkPoint rt = find(points, "rt");
        BuckleMarkPoint rb = find(points, "rb");
        BuckleMarkPoint lb = find(points, "lb");
        assertEquals(150D, lt.getCenterX());
        assertEquals(150D, lt.getCenterY());
        assertEquals(850D, rt.getCenterX());
        assertEquals(150D, rt.getCenterY());
        assertEquals(850D, rb.getCenterX());
        assertEquals(475D, rb.getCenterY());
        assertEquals(150D, lb.getCenterX());
        assertEquals(475D, lb.getCenterY());
    }

    @Test
    void nonSuperWidthSpliceKeepsDefaultOffset() {
        ProductionPiece piece = new ProductionPiece();
        Blood blood = new Blood();
        blood.setX(-1);
        blood.setY(1);
        piece.setBlood(blood);

        List<BuckleMarkPoint> points = strategy.buildMarkPoints(context(piece, "四周打扣"), 1000D, 500D);

        BuckleMarkPoint lt = find(points, "lt");
        BuckleMarkPoint rb = find(points, "rb");
        assertEquals(25D, lt.getCenterX());
        assertEquals(25D, lt.getCenterY());
        assertEquals(975D, rb.getCenterX());
        assertEquals(475D, rb.getCenterY());
    }

    private BuckleProcessContext context(ProductionPiece piece, String nodeName) {
        ProcedureFlowNode node = new ProcedureFlowNode();
        node.setNodeName(nodeName);
        ProcedureFlow procedureFlow = new ProcedureFlow();
        procedureFlow.setNodes(List.of(node));
        BuckleProcessContext context = new BuckleProcessContext();
        context.setProductionPiece(piece);
        context.setProcedureFlow(procedureFlow);
        return context;
    }

    private BuckleMarkPoint find(List<BuckleMarkPoint> points, String suffix) {
        return points.stream()
                .filter(point -> suffix.equals(point.getSuffix()))
                .findFirst()
                .orElseThrow();
    }
}

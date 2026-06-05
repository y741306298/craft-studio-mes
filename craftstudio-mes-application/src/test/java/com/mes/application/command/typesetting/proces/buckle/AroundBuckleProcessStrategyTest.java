package com.mes.application.command.typesetting.proces.buckle;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.Blood;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AroundBuckleProcessStrategyTest {
    private final AroundBuckleProcessStrategy strategy = new AroundBuckleProcessStrategy(null, null);

    @Test
    void superWidthSpliceSkipsBloodAndCoveredEdges() {
        ProductionPiece piece = new ProductionPiece();
        piece.setSeq(1);
        piece.setGroup("# 1-3");
        Blood blood = new Blood();
        blood.setX(-1);
        blood.setY(1);
        piece.setBlood(blood);

        List<BuckleMarkPoint> points = strategy.buildMarkPoints(context(piece, "超幅拼接"), 1000D, 500D);

        BuckleMarkPoint rb = find(points, "rb");
        BuckleMarkPoint lb = find(points, "lb");
        assertEquals(4, points.size());
        assertTrue(points.stream().allMatch(point -> point.getCenterY() == 475D));
        assertEquals(850D, rb.getCenterX());
        assertEquals(475D, rb.getCenterY());
        assertEquals(150D, lb.getCenterX());
        assertEquals(475D, lb.getCenterY());
    }

    @Test
    void verticalCutUsesLeftAndRightCoveredEdges() {
        ProductionPiece piece = new ProductionPiece();
        piece.setSeq(1);
        piece.setGroup("# 1-3");
        Blood blood = new Blood();
        blood.setX(-1);
        blood.setY(0);
        piece.setBlood(blood);

        List<BuckleMarkPoint> points = strategy.buildMarkPoints(context(piece, "超幅拼接"), 1000D, 500D);

        BuckleMarkPoint lt = find(points, "lt");
        BuckleMarkPoint rt = find(points, "rt");
        BuckleMarkPoint rb = find(points, "rb");
        BuckleMarkPoint lb = find(points, "lb");
        assertEquals(150D, lt.getCenterX());
        assertEquals(25D, lt.getCenterY());
        assertEquals(850D, rt.getCenterX());
        assertEquals(25D, rt.getCenterY());
        assertEquals(850D, rb.getCenterX());
        assertEquals(475D, rb.getCenterY());
        assertEquals(150D, lb.getCenterX());
        assertEquals(475D, lb.getCenterY());
        assertEquals(8, points.size());
        assertTrue(points.stream().allMatch(point -> point.getCenterY() == 25D || point.getCenterY() == 475D));
    }


    @Test
    void rightBloodAndLeftCoveredKeepsTopAndBottomCornersAndSpacing() {
        ProductionPiece piece = new ProductionPiece();
        piece.setSeq(3);
        piece.setGroup("# 1-3");
        Blood blood = new Blood();
        blood.setX(1);
        blood.setY(0);
        piece.setBlood(blood);

        List<BuckleMarkPoint> points = strategy.buildMarkPoints(context(piece, "超幅拼接"), 1000D, 500D);

        BuckleMarkPoint lt = find(points, "lt");
        BuckleMarkPoint rt = find(points, "rt");
        BuckleMarkPoint rb = find(points, "rb");
        BuckleMarkPoint lb = find(points, "lb");
        assertEquals(150D, lt.getCenterX());
        assertEquals(25D, lt.getCenterY());
        assertEquals(850D, rt.getCenterX());
        assertEquals(25D, rt.getCenterY());
        assertEquals(850D, rb.getCenterX());
        assertEquals(475D, rb.getCenterY());
        assertEquals(150D, lb.getCenterX());
        assertEquals(475D, lb.getCenterY());
        assertEquals(8, points.size());
        assertEquals(2, points.stream().filter(point -> point.getSuffix().startsWith("top-")).count());
        assertEquals(2, points.stream().filter(point -> point.getSuffix().startsWith("bottom-")).count());
        assertEquals(0, points.stream().filter(point -> point.getSuffix().startsWith("left-")).count());
        assertEquals(0, points.stream().filter(point -> point.getSuffix().startsWith("right-")).count());
    }

    @Test
    void horizontalCutUsesTopAndBottomCoveredEdges() {
        ProductionPiece piece = new ProductionPiece();
        piece.setSeq(1);
        piece.setGroup("# 1-3");
        Blood blood = new Blood();
        blood.setX(0);
        blood.setY(1);
        piece.setBlood(blood);

        List<BuckleMarkPoint> points = strategy.buildMarkPoints(context(piece, "超幅拼接"), 1000D, 500D);

        BuckleMarkPoint lt = find(points, "lt");
        BuckleMarkPoint rt = find(points, "rt");
        BuckleMarkPoint rb = find(points, "rb");
        BuckleMarkPoint lb = find(points, "lb");
        assertEquals(25D, lt.getCenterX());
        assertEquals(150D, lt.getCenterY());
        assertEquals(975D, rt.getCenterX());
        assertEquals(150D, rt.getCenterY());
        assertEquals(975D, rb.getCenterX());
        assertEquals(350D, rb.getCenterY());
        assertEquals(25D, lb.getCenterX());
        assertEquals(350D, lb.getCenterY());
        assertEquals(4, points.size());
        assertTrue(points.stream().allMatch(point -> point.getCenterX() == 25D || point.getCenterX() == 975D));
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

    @Test
    void outsideBuckleUses75MillimeterOffsetFromOutermostBounds() {
        ProductionPiece piece = new ProductionPiece();

        List<BuckleMarkPoint> points = strategy.buildMarkPoints(context(piece, "四周打扣", "画外打扣"), 1000D, 500D);

        BuckleMarkPoint lt = find(points, "lt");
        BuckleMarkPoint rb = find(points, "rb");
        assertEquals(75D, lt.getCenterX());
        assertEquals(75D, lt.getCenterY());
        assertEquals(925D, rb.getCenterX());
        assertEquals(425D, rb.getCenterY());
    }

    @Test
    void insideBuckleKeepsDefaultOffsetEvenWhenOutsideNodeExists() {
        ProductionPiece piece = new ProductionPiece();

        List<BuckleMarkPoint> points = strategy.buildMarkPoints(context(piece, "四周打扣", "画外打扣", "画内打扣"), 1000D, 500D);

        BuckleMarkPoint lt = find(points, "lt");
        BuckleMarkPoint rb = find(points, "rb");
        assertEquals(25D, lt.getCenterX());
        assertEquals(25D, lt.getCenterY());
        assertEquals(975D, rb.getCenterX());
        assertEquals(475D, rb.getCenterY());
    }

    private BuckleProcessContext context(ProductionPiece piece, String... nodeNames) {
        ProcedureFlow procedureFlow = new ProcedureFlow();
        procedureFlow.setNodes(List.of(nodeNames).stream()
                .map(nodeName -> {
                    ProcedureFlowNode node = new ProcedureFlowNode();
                    node.setNodeName(nodeName);
                    return node;
                })
                .toList());
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

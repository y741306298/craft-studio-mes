package com.mes.application.command.typesetting.proces.buckle;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.Blood;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * “四周打扣”实体策略：沿非出血/非被出血边写入扣点，相邻扣点间距不超过 300mm。
 *
 * <p>该策略在订单预处理阶段把扣点直接写入生产工件 mask SVG，与“四角打扣”的处理方式保持一致；
 * 因此 confirmLayout / confirmPrint 阶段只需要消费已经带扣点的生产工件，不再临时按 nestedSvg 追加印版 mark。</p>
 */
@Service
public class AroundBuckleProcessStrategy extends AbstractBuckleProcessStrategy {
    private static final String SUPER_WIDTH_SPLICE_NODE_NAME = "超幅拼接";
    private static final double BLEED_EDGE_OFFSET_MM = 150D;
    private static final Pattern MAX_SEQ_PATTERN = Pattern.compile("#\\s*\\d+-(\\d+)");

    public AroundBuckleProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        super(restTemplate, ossTagUploadService);
    }

    @Override
    protected String nodeName() {
        return "四周打扣";
    }

    @Override
    protected String markKeyPrefix() {
        return "around-buckle-point";
    }

    @Override
    protected List<BuckleMarkPoint> buildMarkPoints(BuckleProcessContext context, double width, double height) {
        ProductionPiece piece = context == null ? null : context.getProductionPiece();
        ProcedureFlow procedureFlow = context == null ? null : context.getProcedureFlow();
        return buildAroundMarkPoints(width, height, resolveBleedSides(piece, procedureFlow));
    }

    @Override
    protected List<BuckleMarkPoint> buildMarkPoints(double width, double height) {
        return buildAroundMarkPoints(width, height, EnumSet.noneOf(BuckleEdge.class));
    }

    private List<BuckleMarkPoint> buildAroundMarkPoints(double width, double height, Set<BuckleEdge> bleedSides) {
        OffsetBox offsets = resolveOffsets(bleedSides);
        Map<String, BuckleMarkPoint> points = new LinkedHashMap<>();
        CornerPoint lt = new CornerPoint("lt", offsets.left, offsets.top);
        CornerPoint rt = new CornerPoint("rt", width - offsets.right, offsets.top);
        CornerPoint rb = new CornerPoint("rb", width - offsets.right, height - offsets.bottom);
        CornerPoint lb = new CornerPoint("lb", offsets.left, height - offsets.bottom);

        addNonBleedEdgeMarks(points, bleedSides, BuckleEdge.TOP, "top", lt, rt);
        addNonBleedEdgeMarks(points, bleedSides, BuckleEdge.RIGHT, "right", rt, rb);
        addNonBleedEdgeMarks(points, bleedSides, BuckleEdge.BOTTOM, "bottom", rb, lb);
        addNonBleedEdgeMarks(points, bleedSides, BuckleEdge.LEFT, "left", lb, lt);
        return new ArrayList<>(points.values());
    }

    private OffsetBox resolveOffsets(Set<BuckleEdge> bleedSides) {
        OffsetBox offsets = new OffsetBox();
        offsets.top = resolveOffset(bleedSides, BuckleEdge.TOP);
        offsets.right = resolveOffset(bleedSides, BuckleEdge.RIGHT);
        offsets.bottom = resolveOffset(bleedSides, BuckleEdge.BOTTOM);
        offsets.left = resolveOffset(bleedSides, BuckleEdge.LEFT);
        return offsets;
    }

    private double resolveOffset(Set<BuckleEdge> bleedSides, BuckleEdge edge) {
        return bleedSides != null && bleedSides.contains(edge) ? BLEED_EDGE_OFFSET_MM : EDGE_OFFSET_MM;
    }

    private void addNonBleedEdgeMarks(Map<String, BuckleMarkPoint> result, Set<BuckleEdge> bleedSides, BuckleEdge edge, String edgeName, CornerPoint start, CornerPoint end) {
        if (bleedSides != null && bleedSides.contains(edge)) {
            return;
        }
        // 角点跟随当前非出血边保留；相邻边即使是出血/被出血边，也不影响该非出血边的端点打扣。
        addPoint(result, start.toBuckleMarkPoint());
        addEdgeMarks(result, edgeName, start, end);
        addPoint(result, end.toBuckleMarkPoint());
    }

    private void addEdgeMarks(Map<String, BuckleMarkPoint> result, String edgeName, CornerPoint start, CornerPoint end) {
        double edgeLength = distance(start.x, start.y, end.x, end.y);
        if (edgeLength <= MAX_BUCKLE_SPACING_MM) {
            return;
        }
        int segmentCount = (int) Math.ceil(edgeLength / MAX_BUCKLE_SPACING_MM);
        double spacing = edgeLength / segmentCount;
        double unitX = (end.x - start.x) / edgeLength;
        double unitY = (end.y - start.y) / edgeLength;
        for (int i = 1; i < segmentCount; i++) {
            addPoint(result, new BuckleMarkPoint(edgeName + "-" + i, start.x + unitX * spacing * i, start.y + unitY * spacing * i));
        }
    }

    private void addPoint(Map<String, BuckleMarkPoint> result, BuckleMarkPoint point) {
        result.putIfAbsent(point.getCenterX() + "," + point.getCenterY(), point);
    }

    private Set<BuckleEdge> resolveBleedSides(ProductionPiece piece, ProcedureFlow procedureFlow) {
        Set<BuckleEdge> bleedSides = EnumSet.noneOf(BuckleEdge.class);
        if (!hasProcedureNode(piece, SUPER_WIDTH_SPLICE_NODE_NAME) && !hasProcedureNode(procedureFlow, SUPER_WIDTH_SPLICE_NODE_NAME)) {
            return bleedSides;
        }
        Blood blood = piece == null ? null : piece.getBlood();
        addBleedSidesFromBlood(bleedSides, blood);
        bleedSides.addAll(resolveBleedSidesFromSequence(piece, blood));
        return bleedSides;
    }

    /**
     * 根据算法回写的 blood 方向确认超幅拼接主动出血边。
     *
     * <p>该映射与留白策略保持一致：x 正/负分别表示右/左出血，y 正/负分别表示上/下出血；
     * 被出血边由分片顺序和切割方向补充。</p>
     */
    private void addBleedSidesFromBlood(Set<BuckleEdge> bleedSides, Blood blood) {
        if (blood == null) {
            return;
        }
        Integer x = blood.getX();
        Integer y = blood.getY();
        if (x != null && x > 0) {
            bleedSides.add(BuckleEdge.RIGHT);
        } else if (x != null && x < 0) {
            bleedSides.add(BuckleEdge.LEFT);
        }
        if (y != null && y > 0) {
            bleedSides.add(BuckleEdge.TOP);
        } else if (y != null && y < 0) {
            bleedSides.add(BuckleEdge.BOTTOM);
        }
    }

    /**
     * 按分片顺序推断被出血边。
     *
     * <p>blood.y 为 0 时代表竖切，分片沿左右方向相邻，按原有左右边补充；blood.x 为 0 时代表横切，
     * 分片沿上下方向相邻，需要按上下边补充，避免横切场景继续套用竖切的左右边逻辑。</p>
     */
    private Set<BuckleEdge> resolveBleedSidesFromSequence(ProductionPiece piece, Blood blood) {
        Set<BuckleEdge> bleedSides = EnumSet.noneOf(BuckleEdge.class);
        Integer currentSeq = piece == null ? null : piece.getSeq();
        Integer maxSeq = piece == null ? null : extractMaxSeqInGroup(piece.getGroup());
        if (currentSeq == null || maxSeq == null || maxSeq <= 0) {
            return bleedSides;
        }
        BuckleEdge firstCoveredEdge = resolveFirstCoveredEdge(blood);
        BuckleEdge lastCoveredEdge = oppositeEdge(firstCoveredEdge);
        if (currentSeq == 1 || (currentSeq > 1 && currentSeq < maxSeq)) {
            bleedSides.add(firstCoveredEdge);
        }
        if (currentSeq.intValue() == maxSeq.intValue() || (currentSeq > 1 && currentSeq < maxSeq)) {
            bleedSides.add(lastCoveredEdge);
        }
        return bleedSides;
    }

    private BuckleEdge resolveFirstCoveredEdge(Blood blood) {
        if (blood != null && isZero(blood.getX()) && isNonZero(blood.getY())) {
            return BuckleEdge.BOTTOM;
        }
        return BuckleEdge.RIGHT;
    }

    private BuckleEdge oppositeEdge(BuckleEdge edge) {
        switch (edge) {
            case BOTTOM:
                return BuckleEdge.TOP;
            case TOP:
                return BuckleEdge.BOTTOM;
            case LEFT:
                return BuckleEdge.RIGHT;
            case RIGHT:
            default:
                return BuckleEdge.LEFT;
        }
    }

    private boolean isZero(Integer value) {
        return value != null && value == 0;
    }

    private boolean isNonZero(Integer value) {
        return value != null && value != 0;
    }

    private boolean hasProcedureNode(ProductionPiece piece, String nodeName) {
        return piece != null && hasProcedureNode(piece.getProcedureFlow(), nodeName);
    }

    private boolean hasProcedureNode(ProcedureFlow procedureFlow, String nodeName) {
        if (procedureFlow == null || procedureFlow.getNodes() == null) {
            return false;
        }
        for (ProcedureFlowNode node : procedureFlow.getNodes()) {
            if (node != null && nodeName.equals(node.getNodeName())) {
                return true;
            }
        }
        return false;
    }

    private Integer extractMaxSeqInGroup(String group) {
        if (StringUtils.isBlank(group)) {
            return null;
        }
        Matcher matcher = MAX_SEQ_PATTERN.matcher(group);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (Exception e) {
            return null;
        }
    }

    private double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static class OffsetBox {
        private double top;
        private double right;
        private double bottom;
        private double left;
    }

    private static class CornerPoint {
        private final String suffix;
        private final double x;
        private final double y;

        private CornerPoint(String suffix, double x, double y) {
            this.suffix = suffix;
            this.x = x;
            this.y = y;
        }

        private BuckleMarkPoint toBuckleMarkPoint() {
            return new BuckleMarkPoint(suffix, x, y);
        }
    }
}

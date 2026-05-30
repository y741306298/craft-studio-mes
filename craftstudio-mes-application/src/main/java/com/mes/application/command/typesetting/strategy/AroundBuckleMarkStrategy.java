package com.mes.application.command.typesetting.strategy;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AroundBuckleMarkStrategy implements SpecialCraftMarkStrategy {

    private static final String NODE_NAME = "四周打扣";
    private static final String SUPER_WIDTH_SPLICE_NODE_NAME = "超幅拼接";
    private static final String MARK_IMG = "https://craftstudio-mes-prod.oss-cn-hangzhou.aliyuncs.com/basetag/point.png";
    private static final BigDecimal MARK_SIZE = BigDecimal.valueOf(8D);
    private static final int EDGE_OFFSET_MM = 25;
    private static final int BLEED_EDGE_OFFSET_MM = 150;
    private static final int MAX_BUCKLE_SPACING_MM = 300;
    private static final double EPSILON = 1e-6;
    private static final Pattern PATH_NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");
    private static final Pattern MAX_SEQ_PATTERN = Pattern.compile("#\\s*\\d+-(\\d+)");

    private final RestTemplate restTemplate;
    private final ProductionPieceService productionPieceService;

    public AroundBuckleMarkStrategy(RestTemplate restTemplate, ProductionPieceService productionPieceService) {
        this.restTemplate = restTemplate;
        this.productionPieceService = productionPieceService;
    }

    @Override
    public void apply(TypesettingInfo typesettingInfo, FormeGenerationRequest formeRequest) {
        boolean hasTypesettingNode = containsNode(typesettingInfo, NODE_NAME);
        if (!hasTypesettingNode && (typesettingInfo == null || typesettingInfo.getTypesettingCells() == null)) {
            return;
        }
        if (typesettingInfo == null || typesettingInfo.getElement() == null || StringUtils.isBlank(typesettingInfo.getElement().getNestedSvg())) {
            return;
        }
        String svgContent = fetchSvgContent(typesettingInfo.getElement().getNestedSvg());
        if (StringUtils.isBlank(svgContent)) {
            return;
        }
        List<FormeGenerationRequest.Mark> marks = ensureMarkList(formeRequest);
        int beforeCount = marks.size();
        int productionCellCount = 0;

        for (TypesettingSourceCell cell : filterProductionPieceCells(typesettingInfo.getTypesettingCells())) {
            ProductionPiece piece = findProductionPiece(cell.getSourceId());
            if (!hasTypesettingNode && !hasProcedureNode(piece, NODE_NAME)) {
                continue;
            }
            productionCellCount++;
            String pathD = findPathDById(svgContent, cell.getSourceId());
            if (StringUtils.isBlank(pathD)) {
                log.info("四周打扣策略未命中path: typesettingId={}, sourceId={}", typesettingInfo.getTypesettingId(), cell.getSourceId());
                continue;
            }
            RotatedRect rect = parseRectangle(pathD);
            if (rect == null || rect.width < EDGE_OFFSET_MM * 2 || rect.height < EDGE_OFFSET_MM * 2) {
                log.info("四周打扣策略跳过非矩形或尺寸不足: typesettingId={}, sourceId={}", typesettingInfo.getTypesettingId(), cell.getSourceId());
                continue;
            }
            marks.addAll(buildAroundMarks(rect, typesettingInfo, formeRequest, resolveBleedSides(piece)));
        }
        log.info("四周打扣策略执行完成: typesettingId={}, productionCellCount={}, addMarkCount={}",
                typesettingInfo.getTypesettingId(), productionCellCount, marks.size() - beforeCount);
    }

    private boolean containsNode(TypesettingInfo typesettingInfo, String nodeName) {
        if (typesettingInfo == null || typesettingInfo.getProcedureFlow() == null || typesettingInfo.getProcedureFlow().getNodes() == null) {
            return false;
        }
        for (ProcedureFlowNode node : typesettingInfo.getProcedureFlow().getNodes()) {
            if (node != null && nodeName.equals(node.getNodeName())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasProcedureNode(ProductionPiece piece, String nodeName) {
        if (piece == null || piece.getProcedureFlow() == null || piece.getProcedureFlow().getNodes() == null) {
            return false;
        }
        for (ProcedureFlowNode node : piece.getProcedureFlow().getNodes()) {
            if (node != null && nodeName.equals(node.getNodeName())) {
                return true;
            }
        }
        return false;
    }

    private ProductionPiece findProductionPiece(String sourceId) {
        try {
            return productionPieceService.findById(sourceId);
        } catch (Exception e) {
            log.warn("查询生产工件失败: sourceId={}, error={}", sourceId, e.getMessage());
            return null;
        }
    }

    private Set<EdgeType> resolveBleedSides(ProductionPiece piece) {
        Set<EdgeType> bleedSides = EnumSet.noneOf(EdgeType.class);
        if (!hasProcedureNode(piece, SUPER_WIDTH_SPLICE_NODE_NAME)) {
            return bleedSides;
        }
        Integer currentSeq = piece == null ? null : piece.getSeq();
        Integer maxSeq = piece == null ? null : extractMaxSeqInGroup(piece.getGroup());
        if (currentSeq == null || maxSeq == null || maxSeq <= 0) {
            return bleedSides;
        }
        if (currentSeq == 1 || (currentSeq > 1 && currentSeq < maxSeq)) {
            bleedSides.add(EdgeType.RIGHT);
        }
        if (currentSeq.intValue() == maxSeq.intValue() || (currentSeq > 1 && currentSeq < maxSeq)) {
            bleedSides.add(EdgeType.LEFT);
        }
        return bleedSides;
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

    private List<TypesettingSourceCell> filterProductionPieceCells(List<TypesettingSourceCell> typesettingCells) {
        if (typesettingCells == null) {
            return List.of();
        }
        List<TypesettingSourceCell> result = new ArrayList<>();
        for (TypesettingSourceCell cell : typesettingCells) {
            if (cell != null && TypesettingSourceType.PART.getCode().equals(cell.getSourceType()) && StringUtils.isNotBlank(cell.getSourceId())) {
                result.add(cell);
            }
        }
        return result;
    }

    private String findPathDById(String svgContent, String id) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(svgContent.getBytes(StandardCharsets.UTF_8)));

            NodeList pathNodes = document.getElementsByTagName("path");
            for (int i = 0; i < pathNodes.getLength(); i++) {
                Element path = (Element) pathNodes.item(i);
                if (id.equals(path.getAttribute("id"))
                        || id.equals(path.getAttribute("data-id"))
                        || id.equals(path.getAttribute("data-source-id"))) {
                    return path.getAttribute("d");
                }
            }

            NodeList groupNodes = document.getElementsByTagName("g");
            for (int i = 0; i < groupNodes.getLength(); i++) {
                Element group = (Element) groupNodes.item(i);
                if (!(id.equals(group.getAttribute("id"))
                        || id.equals(group.getAttribute("data-id"))
                        || id.equals(group.getAttribute("data-source-id")))) {
                    continue;
                }
                NodeList children = group.getElementsByTagName("path");
                if (children.getLength() > 0) {
                    Element firstPath = (Element) children.item(0);
                    return firstPath.getAttribute("d");
                }
            }
        } catch (Exception e) {
            log.warn("解析 nestedSvg 失败: {}", e.getMessage());
        }
        return null;
    }

    private RotatedRect parseRectangle(String pathD) {
        if (StringUtils.isBlank(pathD)) {
            return null;
        }
        String normalized = pathD.replace(',', ' ').trim();
        String commandOnly = normalized.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (!"MLLLZ".equals(commandOnly)) {
            return null;
        }
        List<Double> values = extractNumbers(normalized);
        if (values.size() != 8) {
            return null;
        }
        Point p1 = new Point(values.get(0), values.get(1));
        Point p2 = new Point(values.get(2), values.get(3));
        Point p3 = new Point(values.get(4), values.get(5));
        Point p4 = new Point(values.get(6), values.get(7));
        if (!isValidNumber(p1.x + p1.y + p2.x + p2.y + p3.x + p3.y + p4.x + p4.y)) {
            return null;
        }
        double w1 = distance(p1, p2);
        double h1 = distance(p2, p3);
        double w2 = distance(p3, p4);
        double h2 = distance(p4, p1);
        if (w1 < EPSILON || h1 < EPSILON || w2 < EPSILON || h2 < EPSILON) {
            return null;
        }
        if (Math.abs(w1 - w2) > 1e-3 || Math.abs(h1 - h2) > 1e-3) {
            return null;
        }
        Point v12 = vector(p1, p2);
        Point v23 = vector(p2, p3);
        if (Math.abs(dot(v12, v23)) > 1e-3 * (length(v12) * length(v23))) {
            return null;
        }
        return new RotatedRect(new Point[]{p1, p2, p3, p4}, w1, h1);
    }

    private List<FormeGenerationRequest.Mark> buildAroundMarks(RotatedRect rect, TypesettingInfo typesettingInfo,
                                                               FormeGenerationRequest formeRequest, Set<EdgeType> bleedSides) {
        List<FormeGenerationRequest.Mark> result = new ArrayList<>();
        Point[] bucklePoints = buildCornerBucklePoints(rect, bleedSides);
        for (Point point : bucklePoints) {
            result.add(createMark(point, typesettingInfo, formeRequest));
        }
        for (int i = 0; i < bucklePoints.length; i++) {
            Point start = bucklePoints[i];
            Point end = bucklePoints[(i + 1) % bucklePoints.length];
            addEdgeMarks(result, start, end, typesettingInfo, formeRequest);
        }
        return result;
    }

    private Point[] buildCornerBucklePoints(RotatedRect rect, Set<EdgeType> bleedSides) {
        Point[] points = rect.points;
        Point[] result = new Point[points.length];
        for (int i = 0; i < points.length; i++) {
            Point current = points[i];
            Point prev = points[(i - 1 + points.length) % points.length];
            Point next = points[(i + 1) % points.length];
            Point inwardA = normalize(vector(current, prev));
            Point inwardB = normalize(vector(current, next));
            double offsetA = resolveOffset(rect, current, prev, bleedSides);
            double offsetB = resolveOffset(rect, current, next, bleedSides);
            result[i] = new Point(
                    current.x + inwardA.x * offsetA + inwardB.x * offsetB,
                    current.y + inwardA.y * offsetA + inwardB.y * offsetB
            );
        }
        return result;
    }

    private double resolveOffset(RotatedRect rect, Point a, Point b, Set<EdgeType> bleedSides) {
        EdgeType edgeType = classifyEdge(rect, a, b);
        if (bleedSides != null && bleedSides.contains(edgeType)) {
            return BLEED_EDGE_OFFSET_MM;
        }
        return EDGE_OFFSET_MM;
    }

    private EdgeType classifyEdge(RotatedRect rect, Point a, Point b) {
        double midX = (a.x + b.x) / 2D;
        double midY = (a.y + b.y) / 2D;
        if (Math.abs(midX - rect.minX) <= Math.abs(midX - rect.maxX)) {
            if (Math.abs(midX - rect.minX) <= Math.min(Math.abs(midY - rect.minY), Math.abs(midY - rect.maxY))) {
                return EdgeType.LEFT;
            }
        } else if (Math.abs(midX - rect.maxX) <= Math.min(Math.abs(midY - rect.minY), Math.abs(midY - rect.maxY))) {
            return EdgeType.RIGHT;
        }
        if (Math.abs(midY - rect.minY) <= Math.abs(midY - rect.maxY)) {
            return EdgeType.TOP;
        }
        return EdgeType.BOTTOM;
    }

    private void addEdgeMarks(List<FormeGenerationRequest.Mark> result, Point start, Point end,
                              TypesettingInfo typesettingInfo, FormeGenerationRequest formeRequest) {
        double edgeLength = distance(start, end);
        if (edgeLength <= MAX_BUCKLE_SPACING_MM) {
            return;
        }
        int segmentCount = (int) Math.ceil(edgeLength / MAX_BUCKLE_SPACING_MM);
        int spacing = (int) Math.ceil(edgeLength / segmentCount);
        Point unit = normalize(vector(start, end));
        for (int distance = spacing; distance < edgeLength - EPSILON; distance += spacing) {
            Point markPoint = new Point(start.x + unit.x * distance, start.y + unit.y * distance);
            result.add(createMark(markPoint, typesettingInfo, formeRequest));
        }
    }

    private FormeGenerationRequest.Mark createMark(Point point, TypesettingInfo typesettingInfo, FormeGenerationRequest formeRequest) {
        int x = convertSvgXToFormeX(point.x, formeRequest);
        int y = convertSvgYToFormeY(point.y, typesettingInfo, formeRequest);
        return createMark(x, y);
    }

    private int convertSvgXToFormeX(double svgX, FormeGenerationRequest formeRequest) {
        int leftMargin = 0;
        if (formeRequest != null && formeRequest.getForme() != null && formeRequest.getForme().getMargin() != null
                && formeRequest.getForme().getMargin().getLeft() != null) {
            leftMargin = formeRequest.getForme().getMargin().getLeft();
        }
        return (int) Math.round(svgX + leftMargin);
    }

    private int convertSvgYToFormeY(double svgY, TypesettingInfo typesettingInfo, FormeGenerationRequest formeRequest) {
        if (typesettingInfo == null || typesettingInfo.getElement() == null || typesettingInfo.getElement().getHeight() == null) {
            return (int) Math.round(svgY);
        }
        double nestedHeight = typesettingInfo.getElement().getHeight().doubleValue();
        int topMargin = 0;
        int bottomMargin = 0;
        if (formeRequest != null && formeRequest.getForme() != null && formeRequest.getForme().getMargin() != null) {
            if (formeRequest.getForme().getMargin().getTop() != null) {
                topMargin = formeRequest.getForme().getMargin().getTop();
            }
            if (formeRequest.getForme().getMargin().getBottom() != null) {
                bottomMargin = formeRequest.getForme().getMargin().getBottom();
            }
        }
        double canvasHeight = nestedHeight + topMargin + bottomMargin;
        double svgYInCanvas = topMargin + svgY;
        return (int) Math.round(canvasHeight - svgYInCanvas);
    }

    private FormeGenerationRequest.Mark createMark(int x, int y) {
        FormeGenerationRequest.Mark mark = new FormeGenerationRequest.Mark();
        mark.setImg(MARK_IMG);
        FormeGenerationRequest.Size size = new FormeGenerationRequest.Size();
        size.setWidth(MARK_SIZE);
        size.setHeight(MARK_SIZE);
        mark.setSize(size);
        FormeGenerationRequest.Position position = new FormeGenerationRequest.Position();
        position.setX(x);
        position.setY(y);
        mark.setPosition(position);
        return mark;
    }

    private List<FormeGenerationRequest.Mark> ensureMarkList(FormeGenerationRequest formeRequest) {
        if (formeRequest == null || formeRequest.getForme() == null) {
            return new ArrayList<>();
        }
        if (formeRequest.getForme().getMarks() == null) {
            formeRequest.getForme().setMarks(new ArrayList<>());
        }
        return formeRequest.getForme().getMarks();
    }

    private String fetchSvgContent(String nestedSvgUrl) {
        try {
            byte[] bytes = restTemplate.getForObject(nestedSvgUrl, byte[].class);
            return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("下载 nestedSvg 失败: {}, error={}", nestedSvgUrl, e.getMessage());
            return null;
        }
    }

    private double parseDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (Exception ignored) {
            return Double.NaN;
        }
    }

    private List<Double> extractNumbers(String pathD) {
        List<Double> values = new ArrayList<>();
        Matcher matcher = PATH_NUMBER_PATTERN.matcher(pathD);
        while (matcher.find()) {
            values.add(parseDouble(matcher.group()));
        }
        return values;
    }

    private boolean isValidNumber(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private double distance(Point p1, Point p2) {
        return length(vector(p1, p2));
    }

    private Point vector(Point from, Point to) {
        return new Point(to.x - from.x, to.y - from.y);
    }

    private double length(Point vector) {
        return Math.sqrt(vector.x * vector.x + vector.y * vector.y);
    }

    private Point normalize(Point vector) {
        double len = length(vector);
        if (len < EPSILON) {
            return new Point(0, 0);
        }
        return new Point(vector.x / len, vector.y / len);
    }

    private double dot(Point a, Point b) {
        return a.x * b.x + a.y * b.y;
    }

    private static class RotatedRect {
        final Point[] points;
        final double width;
        final double height;
        final double minX;
        final double minY;
        final double maxX;
        final double maxY;

        private RotatedRect(Point[] points, double width, double height) {
            this.points = points;
            this.width = width;
            this.height = height;
            double rectMinX = Double.POSITIVE_INFINITY;
            double rectMinY = Double.POSITIVE_INFINITY;
            double rectMaxX = Double.NEGATIVE_INFINITY;
            double rectMaxY = Double.NEGATIVE_INFINITY;
            for (Point point : points) {
                rectMinX = Math.min(rectMinX, point.x);
                rectMinY = Math.min(rectMinY, point.y);
                rectMaxX = Math.max(rectMaxX, point.x);
                rectMaxY = Math.max(rectMaxY, point.y);
            }
            this.minX = rectMinX;
            this.minY = rectMinY;
            this.maxX = rectMaxX;
            this.maxY = rectMaxY;
        }
    }

    private static class Point {
        final double x;
        final double y;

        private Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private enum EdgeType {
        TOP, RIGHT, BOTTOM, LEFT
    }
}

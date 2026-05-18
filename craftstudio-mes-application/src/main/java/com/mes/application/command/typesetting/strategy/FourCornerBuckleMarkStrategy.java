package com.mes.application.command.typesetting.strategy;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FourCornerBuckleMarkStrategy implements SpecialCraftMarkStrategy {

    private static final String NODE_NAME = "四角打扣";
    private static final String MARK_IMG = "https://craftstudio-mes-prod.oss-cn-hangzhou.aliyuncs.com/basetag/point.png";
    private static final BigDecimal MARK_SIZE = BigDecimal.valueOf(0.8D);
    private static final int EDGE_OFFSET_MM = 25;
    private static final double EPSILON = 1e-6;
    private static final Pattern PATH_NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");

    private final RestTemplate restTemplate;

    public FourCornerBuckleMarkStrategy(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void apply(TypesettingInfo typesettingInfo, FormeGenerationRequest formeRequest) {
        if (!containsNode(typesettingInfo)) {
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
            productionCellCount++;
            String pathD = findPathDById(svgContent, cell.getSourceId());
            if (StringUtils.isBlank(pathD)) {
                log.info("四角打扣策略未命中path: typesettingId={}, sourceId={}", typesettingInfo.getTypesettingId(), cell.getSourceId());
                continue;
            }
            RotatedRect rect = parseRectangle(pathD);
            if (rect == null || rect.width < EDGE_OFFSET_MM * 2 || rect.height < EDGE_OFFSET_MM * 2) {
                log.info("四角打扣策略跳过非矩形或尺寸不足: typesettingId={}, sourceId={}", typesettingInfo.getTypesettingId(), cell.getSourceId());
                continue;
            }
            marks.addAll(buildCornerMarks(rect, typesettingInfo));
        }
        log.info("四角打扣策略执行完成: typesettingId={}, productionCellCount={}, addMarkCount={}",
                typesettingInfo.getTypesettingId(), productionCellCount, marks.size() - beforeCount);
    }

    private boolean containsNode(TypesettingInfo typesettingInfo) {
        if (typesettingInfo == null || typesettingInfo.getProcedureFlow() == null || typesettingInfo.getProcedureFlow().getNodes() == null) {
            return false;
        }
        for (ProcedureFlowNode node : typesettingInfo.getProcedureFlow().getNodes()) {
            if (node != null && NODE_NAME.equals(node.getNodeName())) {
                return true;
            }
        }
        return false;
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

            // 1) 先按 path 本身的标识匹配
            NodeList pathNodes = document.getElementsByTagName("path");
            for (int i = 0; i < pathNodes.getLength(); i++) {
                Element path = (Element) pathNodes.item(i);
                if (id.equals(path.getAttribute("id"))
                        || id.equals(path.getAttribute("data-id"))
                        || id.equals(path.getAttribute("data-source-id"))) {
                    return path.getAttribute("d");
                }
            }

            // 2) 兼容示例：id 在 g 上，path 无 id
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
        // 对边长度要近似相等，且相邻边近似垂直
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

    private List<FormeGenerationRequest.Mark> buildCornerMarks(RotatedRect rect, TypesettingInfo typesettingInfo) {
        List<FormeGenerationRequest.Mark> result = new ArrayList<>();
        Point[] points = rect.points;
        for (int i = 0; i < points.length; i++) {
            Point current = points[i];
            Point prev = points[(i - 1 + points.length) % points.length];
            Point next = points[(i + 1) % points.length];
            Point inwardA = normalize(vector(current, prev));
            Point inwardB = normalize(vector(current, next));
            Point markPoint = new Point(
                    current.x + inwardA.x * EDGE_OFFSET_MM + inwardB.x * EDGE_OFFSET_MM,
                    current.y + inwardA.y * EDGE_OFFSET_MM + inwardB.y * EDGE_OFFSET_MM
            );
            int x = (int) Math.round(markPoint.x);
            int y = convertSvgYToFormeY(markPoint.y, typesettingInfo);
            result.add(createMark(x, y));
        }
        return result;
    }

    private int convertSvgYToFormeY(double svgY, TypesettingInfo typesettingInfo) {
        if (typesettingInfo == null || typesettingInfo.getElement() == null || typesettingInfo.getElement().getHeight() == null) {
            return (int) Math.round(svgY);
        }
        double nestedHeight = typesettingInfo.getElement().getHeight().doubleValue();
        return (int) Math.round(nestedHeight - svgY);
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

        private RotatedRect(Point[] points, double width, double height) {
            this.points = points;
            this.width = width;
            this.height = height;
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
}

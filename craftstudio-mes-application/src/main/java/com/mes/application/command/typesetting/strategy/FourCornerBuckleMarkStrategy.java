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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FourCornerBuckleMarkStrategy implements SpecialCraftMarkStrategy {

    private static final String NODE_NAME = "四角打扣";
    private static final String MARK_IMG = "https://craftstudio-mes-prod.oss-cn-hangzhou.aliyuncs.com/basetag/square.svg";
    private static final BigDecimal MARK_SIZE = BigDecimal.valueOf(0.8D);
    private static final int EDGE_OFFSET_MM = 25;
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
            Rect rect = parseRectangle(pathD);
            if (rect == null || rect.width < EDGE_OFFSET_MM * 2 || rect.height < EDGE_OFFSET_MM * 2) {
                log.info("四角打扣策略跳过非矩形或尺寸不足: typesettingId={}, sourceId={}", typesettingInfo.getTypesettingId(), cell.getSourceId());
                continue;
            }
            marks.addAll(buildCornerMarks(rect));
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

    private Rect parseRectangle(String pathD) {
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
        double x1 = values.get(0);
        double y1 = values.get(1);
        double x2 = values.get(2);
        double y2 = values.get(3);
        double x3 = values.get(4);
        double y3 = values.get(5);
        double x4 = values.get(6);
        double y4 = values.get(7);

        if (Double.isNaN(x1 + y1 + x2 + y2 + x3 + y3 + x4 + y4)) {
            return null;
        }
        double minX = Math.min(Math.min(x1, x2), Math.min(x3, x4));
        double maxX = Math.max(Math.max(x1, x2), Math.max(x3, x4));
        double minY = Math.min(Math.min(y1, y2), Math.min(y3, y4));
        double maxY = Math.max(Math.max(y1, y2), Math.max(y3, y4));
        if (Math.abs((maxX - minX) * (maxY - minY)) < 1e-6) {
            return null;
        }
        List<String> pts = Arrays.asList(minX + "," + minY, minX + "," + maxY, maxX + "," + minY, maxX + "," + maxY);
        if (!pts.contains(x1 + "," + y1) || !pts.contains(x2 + "," + y2) || !pts.contains(x3 + "," + y3) || !pts.contains(x4 + "," + y4)) {
            return null;
        }
        return new Rect((int) Math.round(minX), (int) Math.round(minY), (int) Math.round(maxX - minX), (int) Math.round(maxY - minY));
    }

    private List<FormeGenerationRequest.Mark> buildCornerMarks(Rect rect) {
        return Arrays.asList(
                createMark(rect.x + EDGE_OFFSET_MM, rect.y + EDGE_OFFSET_MM),
                createMark(rect.x + rect.width - EDGE_OFFSET_MM, rect.y + EDGE_OFFSET_MM),
                createMark(rect.x + EDGE_OFFSET_MM, rect.y + rect.height - EDGE_OFFSET_MM),
                createMark(rect.x + rect.width - EDGE_OFFSET_MM, rect.y + rect.height - EDGE_OFFSET_MM)
        );
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

    private static class Rect {
        final int x;
        final int y;
        final int width;
        final int height;

        private Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}

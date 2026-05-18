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

@Slf4j
@Service
public class FourCornerBuckleMarkStrategy implements SpecialCraftMarkStrategy {

    private static final String NODE_NAME = "四角打扣";
    private static final String MARK_IMG = "https://craftstudio-mes-prod.oss-cn-hangzhou.aliyuncs.com/basetag/square.svg";
    private static final BigDecimal MARK_SIZE = BigDecimal.valueOf(0.8D);
    private static final int EDGE_OFFSET_MM = 25;

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

        for (TypesettingSourceCell cell : filterProductionPieceCells(typesettingInfo.getTypesettingCells())) {
            String pathD = findPathDById(svgContent, cell.getSourceId());
            if (StringUtils.isBlank(pathD)) {
                continue;
            }
            Rect rect = parseRectangle(pathD);
            if (rect == null || rect.width < EDGE_OFFSET_MM * 2 || rect.height < EDGE_OFFSET_MM * 2) {
                continue;
            }
            marks.addAll(buildCornerMarks(rect));
        }
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
            NodeList pathNodes = document.getElementsByTagName("path");
            for (int i = 0; i < pathNodes.getLength(); i++) {
                Element path = (Element) pathNodes.item(i);
                if (id.equals(path.getAttribute("id"))) {
                    return path.getAttribute("d");
                }
            }
        } catch (Exception e) {
            log.warn("解析 nestedSvg 失败: {}", e.getMessage());
        }
        return null;
    }

    private Rect parseRectangle(String pathD) {
        String normalized = pathD.replaceAll(",", " ").trim();
        String[] tokens = normalized.split("\\s+");
        if (tokens.length != 13 || !("M".equalsIgnoreCase(tokens[0]) && "L".equalsIgnoreCase(tokens[3])
                && "L".equalsIgnoreCase(tokens[6]) && "L".equalsIgnoreCase(tokens[9])
                && "Z".equalsIgnoreCase(tokens[12]))) {
            return null;
        }
        double x1 = parseDouble(tokens[1]);
        double y1 = parseDouble(tokens[2]);
        double x2 = parseDouble(tokens[4]);
        double y2 = parseDouble(tokens[5]);
        double x3 = parseDouble(tokens[7]);
        double y3 = parseDouble(tokens[8]);
        double x4 = parseDouble(tokens[10]);
        double y4 = parseDouble(tokens[11]);

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

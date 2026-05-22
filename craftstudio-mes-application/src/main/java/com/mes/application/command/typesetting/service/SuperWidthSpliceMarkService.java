package com.mes.application.command.typesetting.service;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuperWidthSpliceMarkService {
    private static final String SUPER_WIDTH_SPLICE_NODE_NAME = "超幅拼接";

    private final ProductionPieceService productionPieceService;
    private final RestTemplate restTemplate;
    private final OssTagUploadService ossTagUploadService;

    public void apply(TypesettingInfo typesettingInfo, FormeGenerationRequest formeRequest, String businessId) {
        if (typesettingInfo == null || typesettingInfo.getTypesettingCells() == null || typesettingInfo.getTypesettingCells().isEmpty()
                || formeRequest == null || formeRequest.getForme() == null || StringUtils.isBlank(formeRequest.getForme().getSvgUrl())) {
            return;
        }
        List<ProductionPiece> targetPieces = new ArrayList<>();
        for (TypesettingSourceCell cell : typesettingInfo.getTypesettingCells()) {
            if (cell == null || StringUtils.isBlank(cell.getSourceId())) {
                continue;
            }
            ProductionPiece piece = productionPieceService.findById(cell.getSourceId());
            if (piece == null || piece.getSeq() == null || piece.getGroup() == null) {
                continue;
            }
            if (!hasProcedureNode(piece, SUPER_WIDTH_SPLICE_NODE_NAME)) {
                continue;
            }
            targetPieces.add(piece);
        }
        if (targetPieces.isEmpty()) {
            return;
        }

        String svgContent = fetchUrlContent(formeRequest.getForme().getSvgUrl());
        if (StringUtils.isBlank(svgContent)) {
            return;
        }

        String darkMarkImg = uploadGrayRectMark(businessId, typesettingInfo.getManufacturerMetaId(), typesettingInfo.getTypesettingId());
        ensureFormeMarkList(formeRequest);
        int marginLeft = resolveMarginLeft(formeRequest);
        int marginTop = resolveMarginTop(formeRequest);
        for (ProductionPiece piece : targetPieces) {
            Bounds bounds = extractElementBoundsById(svgContent, piece.getId());
            if (bounds == null) {
                continue;
            }
            int x = Math.max(0, (int) Math.round(bounds.maxX - 20) + marginLeft);
            int topY = Math.max(0, (int) Math.round(bounds.minY) + marginTop);
            int bottomY = Math.max(0, (int) Math.round(bounds.maxY - 6) + marginTop);
            Integer maxSeqInGroup = extractMaxSeqInGroup(piece.getGroup());
            Integer currentSeq = piece.getSeq();
            if (currentSeq == null || maxSeqInGroup == null || maxSeqInGroup <= 0) {
                continue;
            }
            boolean isFirstPiece = currentSeq == 1;
            boolean isLastPiece = currentSeq.intValue() == maxSeqInGroup.intValue();
            boolean isMiddlePiece = currentSeq > 1 && currentSeq < maxSeqInGroup;

            if (isFirstPiece || isMiddlePiece) {
                formeRequest.getForme().getMarks().add(createMark(darkMarkImg, 1, 6, x, topY));
                formeRequest.getForme().getMarks().add(createMark(darkMarkImg, 1, 6, x, bottomY));
            }
            if (isLastPiece || isMiddlePiece) {
                addGroupTextMarks(formeRequest, businessId, typesettingInfo.getManufacturerMetaId(), typesettingInfo.getTypesettingId(), piece.getGroup(), bounds, marginLeft, marginTop);
            }
        }
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

    private String uploadGrayRectMark(String businessId, String manufacturerMetaId, String typesettingId) {
        String subDir = buildMarkSubDir(manufacturerMetaId, typesettingId);
        return ossTagUploadService.uploadTagPng(businessId, createAlternatingStripePng(1, 6), subDir);
    }


    private byte[] createAlternatingStripePng(double width, double height) {
        try {
            int w = (int) Math.ceil(width);
            int h = (int) Math.ceil(height);
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setComposite(AlphaComposite.Src);
            for (int y = 0; y < h; y++) {
                boolean blackBand = y % 2 == 0;
                Color bandColor = blackBand ? Color.BLACK : Color.WHITE;
                g.setColor(new Color(bandColor.getRed(), bandColor.getGreen(), bandColor.getBlue(), 255));
                g.fillRect(0, y, w, 1);
            }
            g.dispose();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("生成黑白交替矩形 PNG 失败", e);
        }
    }

    private void ensureFormeMarkList(FormeGenerationRequest formeRequest) {
        if (formeRequest.getForme().getMarks() == null) {
            formeRequest.getForme().setMarks(new ArrayList<>());
        }
    }

    private FormeGenerationRequest.Mark createMark(String img, double width, double height, int x, int y) {
        FormeGenerationRequest.Mark mark = new FormeGenerationRequest.Mark();
        mark.setImg(img);
        FormeGenerationRequest.Size size = new FormeGenerationRequest.Size();
        size.setWidth(BigDecimal.valueOf(width));
        size.setHeight(BigDecimal.valueOf(height));
        mark.setSize(size);
        FormeGenerationRequest.Position position = new FormeGenerationRequest.Position();
        position.setX(x);
        position.setY(y);
        mark.setPosition(position);
        return mark;
    }

    private String fetchUrlContent(String url) {
        try {
            byte[] bytes = restTemplate.getForObject(url, byte[].class);
            return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("下载 nestedSvg 失败: {}, error={}", url, e.getMessage());
            return null;
        }
    }

    private Bounds extractElementBoundsById(String svgContent, String elementId) {
        if (StringUtils.isBlank(svgContent) || StringUtils.isBlank(elementId)) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(svgContent.getBytes(StandardCharsets.UTF_8)));
            Element root = document.getDocumentElement();
            Element target = findElementById(root, elementId);
            if (target == null) {
                return null;
            }
            Bounds bounds = new Bounds();
            collectBasicBounds(target, bounds);
            return bounds.valid() ? bounds : null;
        } catch (Exception e) {
            log.warn("解析 nestedSvg 元素边界失败: elementId={}, error={}", elementId, e.getMessage());
            return null;
        }
    }

    private Element findElementById(Element root, String elementId) {
        if (root == null) {
            return null;
        }
        if (elementId.equals(root.getAttribute("id"))) {
            return root;
        }
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element found = findElementById((Element) child, elementId);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void collectBasicBounds(Element element, Bounds bounds) {
        String tag = element.getTagName();
        if ("rect".equalsIgnoreCase(tag)) {
            double x = parseDoubleSafe(element.getAttribute("x"), 0D);
            double y = parseDoubleSafe(element.getAttribute("y"), 0D);
            double width = parseDoubleSafe(element.getAttribute("width"), 0D);
            double height = parseDoubleSafe(element.getAttribute("height"), 0D);
            bounds.include(x, y);
            bounds.include(x + width, y + height);
        } else if ("path".equalsIgnoreCase(tag)) {
            Matcher matcher = Pattern.compile("[-+]?\\d*\\.?\\d+").matcher(element.getAttribute("d"));
            boolean isX = true;
            Double x = null;
            while (matcher.find()) {
                double value = Double.parseDouble(matcher.group());
                if (isX) {
                    x = value;
                } else if (x != null) {
                    bounds.include(x, value);
                }
                isX = !isX;
            }
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                collectBasicBounds((Element) child, bounds);
            }
        }
    }

    private double parseDoubleSafe(String value, double defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }



    private int resolveMarginLeft(FormeGenerationRequest formeRequest) {
        if (formeRequest == null || formeRequest.getForme() == null || formeRequest.getForme().getMargin() == null
                || formeRequest.getForme().getMargin().getLeft() == null) {
            return 0;
        }
        return Math.max(0, formeRequest.getForme().getMargin().getLeft());
    }

    private int resolveMarginTop(FormeGenerationRequest formeRequest) {
        if (formeRequest == null || formeRequest.getForme() == null || formeRequest.getForme().getMargin() == null
                || formeRequest.getForme().getMargin().getTop() == null) {
            return 0;
        }
        return Math.max(0, formeRequest.getForme().getMargin().getTop());
    }

    private void addGroupTextMarks(FormeGenerationRequest formeRequest, String businessId, String manufacturerMetaId, String typesettingId, String groupText, Bounds bounds, int marginLeft, int marginTop) {
        int leftX = Math.max(0, (int) Math.round(bounds.minX) + marginLeft);
        int topY = Math.max(0, (int) Math.round(bounds.minY) + marginTop);
        int bottomY = Math.max(0, (int) Math.round(bounds.maxY - 28) + marginTop);
        int rawWidth = Math.max(24, groupText.length() * 8);
        int rawHeight = 24;
        int rotatedWidth = rawHeight;
        int rotatedHeight = rawWidth;
        String markGroup = uploadGroupTextMark(businessId, manufacturerMetaId, typesettingId, groupText, rawWidth, rawHeight);
        formeRequest.getForme().getMarks().add(createMark(markGroup, rotatedWidth, rotatedHeight, leftX, topY));
        formeRequest.getForme().getMarks().add(createMark(markGroup, rotatedWidth, rotatedHeight, leftX, bottomY));
    }

    private String uploadGroupTextMark(String businessId, String manufacturerMetaId, String typesettingId, String text, int width, int height) {
        String subDir = buildMarkSubDir(manufacturerMetaId, typesettingId);
        return ossTagUploadService.uploadTagPng(businessId, createRotatedTwoLineTextPng(width, height, text), subDir);
    }


    private String buildMarkSubDir(String manufacturerMetaId, String typesettingId) {
        String safeManufacturerMetaId = StringUtils.isBlank(manufacturerMetaId) ? "unknown" : manufacturerMetaId;
        String safeTypesettingId = StringUtils.isBlank(typesettingId) ? "unknown" : typesettingId;
        return "mark/" + safeManufacturerMetaId + "/" + safeTypesettingId;
    }
    private byte[] createRotatedTwoLineTextPng(double width, double height, String text) {
        try {
            int w = (int) Math.ceil(width);
            int h = (int) Math.ceil(height);
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, w, h);
            g.setComposite(AlphaComposite.SrcOver);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, h / 2 - 2)));
            FontMetrics fm = g.getFontMetrics();
            int textW = fm.stringWidth(text);
            int textH = fm.getAscent();
            int x = Math.max(0, (w - textW) / 2);
            int firstLineY = Math.max(textH, h / 2 - 2);
            int secondLineY = Math.max(textH + 2, h - 2);
            g.setColor(Color.WHITE);
            g.drawString(text, x, firstLineY);
            g.setColor(createGrayColor(20));
            g.drawString(text, x, secondLineY);
            g.dispose();
            BufferedImage rotated = rotateClockwise90(image);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(rotated, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("生成文字 PNG 失败", e);
        }
    }

    private BufferedImage rotateClockwise90(BufferedImage src) {
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        BufferedImage dst = new BufferedImage(srcH, srcW, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < srcH; y++) {
            for (int x = 0; x < srcW; x++) {
                dst.setRGB(srcH - 1 - y, x, src.getRGB(x, y));
            }
        }
        return dst;
    }

    private Color createGrayColor(int blackPercent) {
        int black = Math.max(0, Math.min(100, blackPercent));
        int v = (int) Math.round(255 * (1 - black / 100.0));
        return new Color(v, v, v);
    }

    private Integer extractMaxSeqInGroup(String group) {
        if (StringUtils.isBlank(group)) {
            return null;
        }
        Matcher matcher = Pattern.compile("#\\s*\\d+-(\\d+)").matcher(group);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (Exception e) {
            return null;
        }
    }

    private static class Bounds {
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;

        private void include(double x, double y) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        private boolean valid() {
            return Double.isFinite(minX) && Double.isFinite(minY)
                    && Double.isFinite(maxX) && Double.isFinite(maxY);
        }
    }
}

package com.mes.application.command.typesetting.service;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 超幅拼接标识服务：负责在排版结果中追加 group 文本与黑白条标识。
 */
public class SuperWidthSpliceMarkService {
    private static final String SUPER_WIDTH_SPLICE_NODE_NAME = "超幅拼接";

    private final ProductionPieceService productionPieceService;
    private final OrderItemService orderItemService;
    private final RestTemplate restTemplate;
    private final OssTagUploadService ossTagUploadService;

    /**
     * 主流程：
     * 1) 过滤出包含“超幅拼接”节点的零件；
     * 2) 拉取 nestedSvg 并解析零件 bounds / data-rotation；
     * 3) seq=1 或 1<seq<m 按历史逻辑放置；
     * 4) 仅 seq=m 按血边方向放置文字与黑白条。
     */
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
            double dataRotation = extractDataRotationById(svgContent, piece.getId());
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
            if (isMiddlePiece) {
                addGroupTextMarks(formeRequest, businessId, typesettingInfo.getManufacturerMetaId(), typesettingInfo.getTypesettingId(), piece.getGroup(), bounds, marginLeft, marginTop, true, 0D);
                addStripeMarks(formeRequest, businessId, darkMarkImg, bounds, marginLeft, marginTop, true, 0D);
            }
            if (isLastPiece) {
                boolean hasVerticalCut = hasVerticalCut(piece);
                addGroupTextMarks(formeRequest, businessId, typesettingInfo.getManufacturerMetaId(), typesettingInfo.getTypesettingId(), piece.getGroup(), bounds, marginLeft, marginTop, hasVerticalCut, dataRotation);
                addStripeMarks(formeRequest, businessId, darkMarkImg, bounds, marginLeft, marginTop, hasVerticalCut, dataRotation);
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

    /**
     * 沿解析出的血边方向放置两份 group 文本（血边两端顶齐）：
     * 白字贴血边，20%黑字离血边 10mm。
     */
    private void addGroupTextMarks(FormeGenerationRequest formeRequest, String businessId, String manufacturerMetaId, String typesettingId, String groupText, Bounds bounds, int marginLeft, int marginTop, boolean hasVerticalCut, double rotationAngle) {
        int rawWidth = Math.max(24, groupText.length() * 8);
        int rawHeight = 12;
        Edge edge = resolveBleedEdge(bounds, marginLeft, marginTop, hasVerticalCut, rotationAngle);
        double edgeAngle = Math.toDegrees(Math.atan2(edge.end.y - edge.start.y, edge.end.x - edge.start.x));

        MarkAsset whiteTextAsset = uploadEdgeAlignedTextMark(businessId, manufacturerMetaId, typesettingId, groupText, rawWidth, rawHeight, edgeAngle, Color.WHITE);
        MarkAsset grayTextAsset = uploadEdgeAlignedTextMark(businessId, manufacturerMetaId, typesettingId, groupText, rawWidth, rawHeight, edgeAngle, createGrayColor(20));

        formeRequest.getForme().getMarks().add(createEdgeMark(whiteTextAsset.img, whiteTextAsset.width, whiteTextAsset.height, edge, 0D, 0D));
        formeRequest.getForme().getMarks().add(createEdgeMark(whiteTextAsset.img, whiteTextAsset.width, whiteTextAsset.height, edge, 1D, 0D));

        formeRequest.getForme().getMarks().add(createEdgeMark(grayTextAsset.img, grayTextAsset.width, grayTextAsset.height, edge, 0D, 10D));
        formeRequest.getForme().getMarks().add(createEdgeMark(grayTextAsset.img, grayTextAsset.width, grayTextAsset.height, edge, 1D, 10D));
    }

    /**
     * 沿解析出的血边方向放置两份黑白条，距离血边内缩 20mm。
     */
    private void addStripeMarks(FormeGenerationRequest formeRequest, String businessId, String darkMarkImg, Bounds bounds, int marginLeft, int marginTop, boolean hasVerticalCut, double rotationAngle) {
        Edge edge = resolveBleedEdge(bounds, marginLeft, marginTop, hasVerticalCut, rotationAngle);
        double edgeAngle = Math.toDegrees(Math.atan2(edge.end.y - edge.start.y, edge.end.x - edge.start.x));
        MarkAsset stripeAsset = uploadEdgeAlignedStripeMark(businessId, darkMarkImg, edgeAngle);
        formeRequest.getForme().getMarks().add(createEdgeMark(stripeAsset.img, stripeAsset.width, stripeAsset.height, edge, 0D, 20D));
        formeRequest.getForme().getMarks().add(createEdgeMark(stripeAsset.img, stripeAsset.width, stripeAsset.height, edge, 1D, 20D));
    }

    /**
     * 在指定边上按比例定位标识中心点，并按法线向内偏移，避免标识出界。
     */
    private FormeGenerationRequest.Mark createEdgeMark(String img, double width, double height, Edge edge, double ratio, double inwardOffset) {
        double safeRatio = Math.max(0D, Math.min(1D, ratio));
        double edgeDx = edge.end.x - edge.start.x;
        double edgeDy = edge.end.y - edge.start.y;
        double edgeLen = Math.hypot(edgeDx, edgeDy);
        PointD tangent = edgeLen < 0.0001D ? new PointD(1D, 0D) : new PointD(edgeDx / edgeLen, edgeDy / edgeLen);

        // 标识沿血边方向的几何半径：用于把端点放置从“中心点”修正为“整图不越端点”。
        double radiusOnTangent = (Math.abs(tangent.x) * width + Math.abs(tangent.y) * height) / 2D;
        double minRatio = edgeLen < 0.0001D ? 0.5D : Math.min(0.5D, radiusOnTangent / edgeLen);
        double maxRatio = 1D - minRatio;
        double clampedRatio = Math.max(minRatio, Math.min(maxRatio, safeRatio));

        // 标识沿法线方向的几何半径：保证标识整体压在血边内侧。
        double radiusOnNormal = (Math.abs(edge.normal.x) * width + Math.abs(edge.normal.y) * height) / 2D;
        double totalInward = radiusOnNormal + Math.max(2D, inwardOffset);

        double cx = edge.start.x + edgeDx * clampedRatio + edge.normal.x * totalInward;
        double cy = edge.start.y + edgeDy * clampedRatio + edge.normal.y * totalInward;
        int x = Math.max(0, (int) Math.round(cx - width / 2D));
        int y = Math.max(0, (int) Math.round(cy - height / 2D));
        return createMark(img, width, height, x, y);
    }

    /**
     * 解析“实际血边”：先按切割类型确定恢复语义边，再按 data-rotation 将边端点绕中心旋转到当前坐标系。
     */
    private Edge resolveBleedEdge(Bounds bounds, int marginLeft, int marginTop, boolean hasVerticalCut, double rotationAngle) {
        double minX = bounds.minX + marginLeft;
        double minY = bounds.minY + marginTop;
        double maxX = bounds.maxX + marginLeft;
        double maxY = bounds.maxY + marginTop;
        PointD center = new PointD((minX + maxX) / 2D, (minY + maxY) / 2D);
        EdgeType baseEdge = hasVerticalCut ? EdgeType.LEFT : EdgeType.TOP;
        PointD r1;
        PointD r2;
        switch (baseEdge) {
            case LEFT:
                r1 = new PointD(minX, minY);
                r2 = new PointD(minX, maxY);
                break;
            case TOP:
            default:
                r1 = new PointD(minX, minY);
                r2 = new PointD(maxX, minY);
                break;
        }
        PointD rotatedR1 = rotateAroundCenter(r1, center, rotationAngle);
        PointD rotatedR2 = rotateAroundCenter(r2, center, rotationAngle);
        PointD edgeDir = new PointD(rotatedR2.x - rotatedR1.x, rotatedR2.y - rotatedR1.y);
        double len = Math.hypot(edgeDir.x, edgeDir.y);
        if (len < 0.0001D) {
            return new Edge(rotatedR1, rotatedR2, new PointD(0, 0), baseEdge);
        }
        PointD normal = new PointD(-edgeDir.y / len, edgeDir.x / len);
        PointD toCenter = new PointD(center.x - (rotatedR1.x + rotatedR2.x) / 2D, center.y - (rotatedR1.y + rotatedR2.y) / 2D);
        if (normal.x * toCenter.x + normal.y * toCenter.y < 0) {
            normal = new PointD(-normal.x, -normal.y);
        }
        return new Edge(rotatedR1, rotatedR2, normal, baseEdge);
    }

    private PointD rotateAroundCenter(PointD point, PointD center, double angle) {
        double rad = Math.toRadians(angle);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double dx = point.x - center.x;
        double dy = point.y - center.y;
        return new PointD(
                center.x + dx * cos - dy * sin,
                center.y + dx * sin + dy * cos
        );
    }

    private double extractDataRotationById(String svgContent, String elementId) {
        if (StringUtils.isBlank(svgContent) || StringUtils.isBlank(elementId)) {
            return 0D;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(svgContent.getBytes(StandardCharsets.UTF_8)));
            Element root = document.getDocumentElement();
            Element target = findElementById(root, elementId);
            if (target == null) {
                return 0D;
            }
            return parseDoubleSafe(target.getAttribute("data-rotation"), 0D);
        } catch (Exception e) {
            log.warn("解析 data-rotation 失败: elementId={}, error={}", elementId, e.getMessage());
            return 0D;
        }
    }

    private String uploadGroupTextMark(String businessId, String manufacturerMetaId, String typesettingId, String text, int width, int height) {
        String subDir = buildMarkSubDir(manufacturerMetaId, typesettingId);
        return ossTagUploadService.uploadTagPng(businessId, createRotatedTwoLineTextPng(width, height, text), subDir);
    }

    private String uploadHorizontalTwoLineTextMark(String businessId, String manufacturerMetaId, String typesettingId, String text, int width, int height) {
        String subDir = buildMarkSubDir(manufacturerMetaId, typesettingId);
        return ossTagUploadService.uploadTagPng(businessId, createTwoLineTextPng(width, height, text), subDir);
    }

    private MarkAsset uploadEdgeAlignedTextMark(String businessId, String manufacturerMetaId, String typesettingId, String text, int width, int height, double angle, Color textColor) {
        String subDir = buildMarkSubDir(manufacturerMetaId, typesettingId);
        BufferedImage base = createSingleLineTextImage(width, height, text, textColor);
        BufferedImage rotated = rotateImageByAngle(base, angle);
        String img = ossTagUploadService.uploadTagPng(businessId, toPng(rotated), subDir);
        return new MarkAsset(img, rotated.getWidth(), rotated.getHeight());
    }

    private MarkAsset uploadEdgeAlignedStripeMark(String businessId, String fallbackImg, double angle) {
        try {
            BufferedImage base = createStripeImage(6, 1);
            BufferedImage rotated = rotateImageByAngle(base, angle);
            String img = ossTagUploadService.uploadTagPng(businessId, toPng(rotated), "mark/dynamic");
            return new MarkAsset(img, rotated.getWidth(), rotated.getHeight());
        } catch (Exception e) {
            return new MarkAsset(fallbackImg, 6, 1);
        }
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

    private byte[] createTwoLineTextPng(double width, double height, String text) {
        try {
            BufferedImage image = createSingleLineTextImage((int) Math.ceil(width), (int) Math.ceil(height), text, Color.WHITE);
            return toPng(image);
        } catch (Exception e) {
            throw new IllegalStateException("生成横向文字 PNG 失败", e);
        }
    }

    private BufferedImage createSingleLineTextImage(int w, int h, String text, Color textColor) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, w, h);
        g.setComposite(AlphaComposite.SrcOver);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, h - 2)));
        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth(text);
        int textH = fm.getAscent();
        int x = Math.max(0, (w - textW) / 2);
        int y = Math.max(textH, Math.min(h - 2, (h + textH) / 2 - 1));
        g.setColor(textColor);
        g.drawString(text, x, y);
        g.dispose();
        return image;
    }

    private BufferedImage createStripeImage(int w, int h) {
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
        return image;
    }

    private BufferedImage rotateImageByAngle(BufferedImage src, double angle) {
        double rad = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(rad));
        double cos = Math.abs(Math.cos(rad));
        int w = src.getWidth();
        int h = src.getHeight();
        int newW = (int) Math.floor(w * cos + h * sin);
        int newH = (int) Math.floor(h * cos + w * sin);
        BufferedImage rotated = new BufferedImage(Math.max(1, newW), Math.max(1, newH), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        g2d.setComposite(AlphaComposite.Src);
        g2d.translate((newW - w) / 2D, (newH - h) / 2D);
        g2d.rotate(rad, w / 2D, h / 2D);
        g2d.drawRenderedImage(src, null);
        g2d.dispose();
        return rotated;
    }

    private byte[] toPng(BufferedImage image) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("PNG 编码失败", e);
        }
    }

    /**
     * 通过 orderItem 的“超幅拼接”节点 param.xs 判断是否存在竖切。
     */
    private boolean hasVerticalCut(ProductionPiece piece) {
        if (piece == null || StringUtils.isBlank(piece.getOrderItemId())) {
            return false;
        }
        OrderItem orderItem = orderItemService.findByOrderItemId(piece.getOrderItemId());
        if (orderItem == null || orderItem.getProcedureFlow() == null || orderItem.getProcedureFlow().getNodes() == null) {
            return false;
        }
        for (ProcedureFlowNode node : orderItem.getProcedureFlow().getNodes()) {
            if (node == null || !SUPER_WIDTH_SPLICE_NODE_NAME.equals(node.getNodeName())
                    || node.getParamConfigs() == null || node.getParamConfigs().isEmpty()) {
                continue;
            }
            MTOProductSpecDTO.ProcessParamConfigDTO config = node.getParamConfigs().get(0);
            if (config == null || config.getParam() == null) {
                continue;
            }
            Object param = config.getParam();
            if (param instanceof Map) {
                Object xs = ((Map<?, ?>) param).get("xs");
                if (xs instanceof List && !((List<?>) xs).isEmpty()) {
                    return true;
                }
            } else {
                Object xs = invokeGetter(param, "getXs");
                if (xs instanceof List && !((List<?>) xs).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private Object invokeGetter(Object target, String methodName) {
        if (target == null || StringUtils.isBlank(methodName)) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception ignore) {
            return null;
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

    private static class PointD {
        private final double x;
        private final double y;

        private PointD(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class Edge {
        private final PointD start;
        private final PointD end;
        private final PointD normal;
        private final EdgeType type;

        private Edge(PointD start, PointD end, PointD normal, EdgeType type) {
            this.start = start;
            this.end = end;
            this.normal = normal;
            this.type = type;
        }
    }

    private static class MarkAsset {
        private final String img;
        private final double width;
        private final double height;

        private MarkAsset(String img, double width, double height) {
            this.img = img;
            this.width = width;
            this.height = height;
        }
    }

    private enum EdgeType {
        TOP, RIGHT, BOTTOM, LEFT
    }
}

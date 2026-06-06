package com.mes.application.command.typesetting.proces.splice;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.Blood;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.shared.utils.IdGenerator;
import com.piliofpala.craftstudio.shared.domain.file.vo.FilePreview;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 超幅拼接回调预处理服务。
 *
 * <p>在蒙版算法 callback 创建 {@link ProductionPiece} 后、生产工件落库前运行，直接把超幅拼接的
 * 出血边/被出血边标识写入当前工件 mask SVG，并回写 {@code productionPiece.marks}。这样后续
 * toLayout 会像留白/打扣预处理后的工件一样，把该工件作为带 marks 的 forme 元素提交排版。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuperWidthSpliceProcessService {
    private static final String SUPER_WIDTH_SPLICE_NODE_NAME = "超幅拼接";
    private static final Pattern SVG_OPEN_PATTERN = Pattern.compile("<svg\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_CLOSE_PATTERN = Pattern.compile("</svg\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_GROUP_PATTERN = Pattern.compile("<g\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_RECT_PATTERN = Pattern.compile("<rect\\b([^>]*)\\s*/>|<rect\\b([^>]*)>\\s*</rect\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_ATTRIBUTE_PATTERN = Pattern.compile("\\s+([A-Za-z_:][-A-Za-z0-9_:.]*)\\s*=\\s*([\"']).*?\\2", Pattern.CASE_INSENSITIVE);
    private static final Pattern MAX_SEQ_PATTERN = Pattern.compile("#\\s*\\d+-(\\d+)");

    private final RestTemplate restTemplate;
    private final OssTagUploadService ossTagUploadService;

    public void process(OrderItem orderItem, ProcedureFlow procedureFlow, ProductionPiece piece) {
        if (orderItem == null || procedureFlow == null || piece == null || !hasNode(procedureFlow, SUPER_WIDTH_SPLICE_NODE_NAME)) {
            return;
        }
        if (piece.getSeq() == null || StringUtils.isBlank(piece.getGroup())
                || piece.getMaskImageFile() == null || StringUtils.isBlank(piece.getMaskImageFile().getRawFile())) {
            return;
        }
        double width = piece.getWidth() == null ? 0D : piece.getWidth();
        double height = piece.getHeight() == null ? 0D : piece.getHeight();
        if (width <= 0D || height <= 0D) {
            return;
        }
        String originalMaskUrl = piece.getMaskImageFile().getRawFile();
        String originalSvg = resolveSvg(originalMaskUrl);
        if (StringUtils.isBlank(originalSvg) || originalSvg.contains("super-width-splice-")) {
            updateMarksForExistingSvg(orderItem, piece);
            return;
        }

        String pieceMongoId = ensureProductionPieceMongoId(piece);
        String productionPieceId = ensureProductionPieceBusinessId(piece);
        String manufacturerMetaId = resolveManufacturerMetaId(orderItem, piece);
        String markSubDir = "mark/" + manufacturerMetaId + "/" + productionPieceId + "/";
        String businessId = productionPieceId;

        MarkAssets assets = uploadMarkAssets(businessId, markSubDir, piece.getGroup());
        Set<SpliceEdge> bleedEdges = resolveBleedEdges(piece.getBlood());
        Set<SpliceEdge> coveredEdges = resolveCoveredEdges(piece, piece.getBlood());
        if (bleedEdges.isEmpty() && coveredEdges.isEmpty()) {
            return;
        }

        String marksSvg = buildMarksSvg(pieceMongoId, width, height, piece.getGroup(), bleedEdges, coveredEdges, assets);
        String markedSvg = appendMarksSvg(originalSvg, pieceMongoId, originalMaskUrl, marksSvg);
        String orderItemId = StringUtils.isBlank(orderItem.getOrderItemId()) ? "default" : orderItem.getOrderItemId();
        String uploadPath = "mask/" + manufacturerMetaId + "/" + orderItemId + "/super-width-splice/";
        String newMaskUrl = ossTagUploadService.uploadTagSvg(businessId, markedSvg.getBytes(StandardCharsets.UTF_8), uploadPath);
        updateMaskImageFile(piece, newMaskUrl);
        updateMarks(piece, assets);
        log.info("超幅拼接预处理完成: productionPieceId={}, mask={}", piece.getProductionPieceId(), newMaskUrl);
    }

    private void updateMarksForExistingSvg(OrderItem orderItem, ProductionPiece piece) {
        String productionPieceId = ensureProductionPieceBusinessId(piece);
        String manufacturerMetaId = resolveManufacturerMetaId(orderItem, piece);
        String markSubDir = "mark/" + manufacturerMetaId + "/" + productionPieceId + "/";
        updateMarks(piece, uploadMarkAssets(productionPieceId, markSubDir, piece.getGroup()));
    }

    private MarkAssets uploadMarkAssets(String businessId, String markSubDir, String groupText) {
        String darkMark = ossTagUploadService.uploadTagPng(businessId, createAlternatingStripePng(1, 6), markSubDir);
        int textWidth = Math.max(24, groupText.length() * 8);
        int textHeight = 12;
        Map<SpliceEdge, EdgeAssets> edgeAssets = new EnumMap<>(SpliceEdge.class);
        for (SpliceEdge edge : SpliceEdge.values()) {
            double angle = edge == SpliceEdge.LEFT || edge == SpliceEdge.RIGHT ? 90D : 0D;
            BufferedImage whiteTextImage = rotateImageByAngle(createTextImage(textWidth, textHeight, groupText, Color.WHITE), angle);
            BufferedImage grayTextImage = rotateImageByAngle(createTextImage(textWidth, textHeight, groupText, createGrayColor(20)), angle);
            BufferedImage stripeImage = rotateImageByAngle(createStripeImage(6, 1), angle);
            String whiteText = ossTagUploadService.uploadTagPng(businessId, toPng(whiteTextImage), markSubDir);
            String grayText = ossTagUploadService.uploadTagPng(businessId, toPng(grayTextImage), markSubDir);
            String stripe = ossTagUploadService.uploadTagPng(businessId, toPng(stripeImage), markSubDir);
            edgeAssets.put(edge, new EdgeAssets(stripe, whiteText, grayText, stripeImage.getWidth(), stripeImage.getHeight(),
                    whiteTextImage.getWidth(), whiteTextImage.getHeight()));
        }
        return new MarkAssets(darkMark, edgeAssets, 1D, 6D);
    }

    private String buildMarksSvg(String pieceMongoId,
                                 double width,
                                 double height,
                                 String groupText,
                                 Set<SpliceEdge> bleedEdges,
                                 Set<SpliceEdge> coveredEdges,
                                 MarkAssets assets) {
        StringBuilder builder = new StringBuilder("\n");
        int index = 0;
        for (SpliceEdge edgeType : bleedEdges) {
            Edge edge = edge(edgeType, width, height);
            builder.append(buildRectMarkGroup("super-width-splice-bleed-" + edgeType.name().toLowerCase(Locale.ROOT) + "-a-" + index + "-" + pieceMongoId,
                    assets.darkMark, rectOnEdge(edge, assets.darkWidth, assets.darkHeight, 0D, 20D)));
            builder.append(buildRectMarkGroup("super-width-splice-bleed-" + edgeType.name().toLowerCase(Locale.ROOT) + "-b-" + index + "-" + pieceMongoId,
                    assets.darkMark, rectOnEdge(edge, assets.darkWidth, assets.darkHeight, 1D, 20D)));
            index++;
        }
        for (SpliceEdge edgeType : coveredEdges) {
            Edge edge = edge(edgeType, width, height);
            EdgeAssets edgeAssets = assets.edgeAssets.get(edgeType);
            if (edgeAssets == null) {
                continue;
            }
            builder.append(buildRectMarkGroup("super-width-splice-text-white-" + edgeType.name().toLowerCase(Locale.ROOT) + "-a-" + index + "-" + pieceMongoId,
                    edgeAssets.whiteText, rectOnEdge(edge, edgeAssets.textWidth, edgeAssets.textHeight, 0D, 0D)));
            builder.append(buildRectMarkGroup("super-width-splice-text-white-" + edgeType.name().toLowerCase(Locale.ROOT) + "-b-" + index + "-" + pieceMongoId,
                    edgeAssets.whiteText, rectOnEdge(edge, edgeAssets.textWidth, edgeAssets.textHeight, 1D, 0D)));
            builder.append(buildRectMarkGroup("super-width-splice-text-gray-" + edgeType.name().toLowerCase(Locale.ROOT) + "-a-" + index + "-" + pieceMongoId,
                    edgeAssets.grayText, rectOnEdge(edge, edgeAssets.textWidth, edgeAssets.textHeight, 0D, 10D)));
            builder.append(buildRectMarkGroup("super-width-splice-text-gray-" + edgeType.name().toLowerCase(Locale.ROOT) + "-b-" + index + "-" + pieceMongoId,
                    edgeAssets.grayText, rectOnEdge(edge, edgeAssets.textWidth, edgeAssets.textHeight, 1D, 10D)));
            builder.append(buildRectMarkGroup("super-width-splice-stripe-" + edgeType.name().toLowerCase(Locale.ROOT) + "-a-" + index + "-" + pieceMongoId,
                    edgeAssets.stripe, rectOnEdge(edge, edgeAssets.stripeWidth, edgeAssets.stripeHeight, 0D, 20D)));
            builder.append(buildRectMarkGroup("super-width-splice-stripe-" + edgeType.name().toLowerCase(Locale.ROOT) + "-b-" + index + "-" + pieceMongoId,
                    edgeAssets.stripe, rectOnEdge(edge, edgeAssets.stripeWidth, edgeAssets.stripeHeight, 1D, 20D)));
            index++;
        }
        return builder.toString();
    }

    private Rect rectOnEdge(Edge edge, double markWidth, double markHeight, double ratio, double inwardOffset) {
        double clampedRatio = Math.max(0D, Math.min(1D, ratio));
        double edgeDx = edge.end.x - edge.start.x;
        double edgeDy = edge.end.y - edge.start.y;
        double totalInward = inwardOffset + Math.max(markWidth, markHeight) / 2D;
        double cx = edge.start.x + edgeDx * clampedRatio + edge.normal.x * totalInward;
        double cy = edge.start.y + edgeDy * clampedRatio + edge.normal.y * totalInward;
        return new Rect(Math.max(0D, cx - markWidth / 2D), Math.max(0D, cy - markHeight / 2D), markWidth, markHeight);
    }

    private String buildRectMarkGroup(String id, String img, Rect rect) {
        return "<g id=\"" + escapeAttr(id) + "\" img=\"" + escapeAttr(img)
                + "\" data-source-name=\"" + escapeAttr(sourceName(img)) + "\" data-forme=\"false\" data-rotation=\"0\">\n"
                + "<path d=\"M" + format(rect.x) + " " + format(rect.y)
                + " H" + format(rect.x + rect.width)
                + " V" + format(rect.y + rect.height)
                + " H" + format(rect.x) + " Z\" fill=\"#111111\" fill-opacity=\"0.82\" fill-rule=\"evenodd\" />\n"
                + "</g>\n";
    }

    private String appendMarksSvg(String originalSvg, String pieceMongoId, String originalContentImg, String marksSvg) {
        String baseSvg = containsGroup(originalSvg) ? originalSvg : rebuildSvgWithOriginalGroup(originalSvg, pieceMongoId, originalContentImg);
        int closeIndex = lastSvgCloseIndex(baseSvg);
        if (closeIndex < 0) {
            return baseSvg;
        }
        return baseSvg.substring(0, closeIndex) + marksSvg + baseSvg.substring(closeIndex);
    }

    private String rebuildSvgWithOriginalGroup(String originalSvg, String pieceMongoId, String originalContentImg) {
        Matcher openMatcher = SVG_OPEN_PATTERN.matcher(originalSvg);
        if (!openMatcher.find()) {
            return originalSvg;
        }
        int closeIndex = lastSvgCloseIndex(originalSvg);
        if (closeIndex < 0 || closeIndex < openMatcher.end()) {
            return originalSvg;
        }
        String prefix = originalSvg.substring(0, openMatcher.end());
        String innerSvg = originalSvg.substring(openMatcher.end(), closeIndex).trim();
        String suffix = originalSvg.substring(closeIndex);
        String groupId = StringUtils.isBlank(pieceMongoId) ? "original-mask" : pieceMongoId;
        return prefix + "\n<g id=\"" + escapeAttr(groupId)
                + "\" img=\"" + escapeAttr(originalContentImg)
                + "\" data-source-name=\"" + escapeAttr(sourceName(originalContentImg))
                + "\" data-forme=\"true\" data-rotation=\"0\">\n"
                + normalizeRectsToPaths(innerSvg) + "\n</g>\n" + suffix;
    }

    private boolean hasNode(ProcedureFlow procedureFlow, String nodeName) {
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

    private Set<SpliceEdge> resolveBleedEdges(Blood blood) {
        Set<SpliceEdge> edges = EnumSet.noneOf(SpliceEdge.class);
        if (blood == null) {
            return edges;
        }
        Integer x = blood.getX();
        Integer y = blood.getY();
        if (x != null && x > 0) {
            edges.add(SpliceEdge.RIGHT);
        } else if (x != null && x < 0) {
            edges.add(SpliceEdge.LEFT);
        }
        if (y != null && y > 0) {
            edges.add(SpliceEdge.TOP);
        } else if (y != null && y < 0) {
            edges.add(SpliceEdge.BOTTOM);
        }
        return edges;
    }

    private Set<SpliceEdge> resolveCoveredEdges(ProductionPiece piece, Blood blood) {
        Set<SpliceEdge> edges = EnumSet.noneOf(SpliceEdge.class);
        Integer currentSeq = piece.getSeq();
        Integer maxSeq = extractMaxSeqInGroup(piece.getGroup());
        if (currentSeq == null || maxSeq == null || maxSeq <= 0) {
            return edges;
        }
        SpliceEdge firstCoveredEdge = resolveFirstCoveredEdge(blood);
        SpliceEdge lastCoveredEdge = oppositeEdge(firstCoveredEdge);
        if (currentSeq == 1 || (currentSeq > 1 && currentSeq < maxSeq)) {
            edges.add(firstCoveredEdge);
        }
        if (currentSeq.intValue() == maxSeq.intValue() || (currentSeq > 1 && currentSeq < maxSeq)) {
            edges.add(lastCoveredEdge);
        }
        return edges;
    }

    private SpliceEdge resolveFirstCoveredEdge(Blood blood) {
        if (blood != null && isZero(blood.getX()) && isNonZero(blood.getY())) {
            return SpliceEdge.BOTTOM;
        }
        return SpliceEdge.RIGHT;
    }

    private SpliceEdge oppositeEdge(SpliceEdge edge) {
        switch (edge) {
            case BOTTOM:
                return SpliceEdge.TOP;
            case TOP:
                return SpliceEdge.BOTTOM;
            case LEFT:
                return SpliceEdge.RIGHT;
            case RIGHT:
            default:
                return SpliceEdge.LEFT;
        }
    }

    private Edge edge(SpliceEdge edgeType, double width, double height) {
        switch (edgeType) {
            case RIGHT:
                return new Edge(new PointD(width, 0D), new PointD(width, height), new PointD(-1D, 0D));
            case BOTTOM:
                return new Edge(new PointD(0D, height), new PointD(width, height), new PointD(0D, -1D));
            case LEFT:
                return new Edge(new PointD(0D, 0D), new PointD(0D, height), new PointD(1D, 0D));
            case TOP:
            default:
                return new Edge(new PointD(0D, 0D), new PointD(width, 0D), new PointD(0D, 1D));
        }
    }

    private String resolveSvg(String url) {
        try {
            byte[] bytes = restTemplate.getForObject(url, byte[].class);
            return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("下载超幅拼接 mask SVG 失败: {}, error={}", url, e.getMessage());
            return null;
        }
    }

    private byte[] createAlternatingStripePng(double width, double height) {
        try {
            BufferedImage image = createStripeImage((int) Math.ceil(width), (int) Math.ceil(height));
            return toPng(image);
        } catch (Exception e) {
            throw new IllegalStateException("生成超幅拼接出血边 PNG 失败", e);
        }
    }

    private BufferedImage createTextImage(int width, int height, String text, Color textColor) {
        BufferedImage image = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, width, height);
        g.setComposite(AlphaComposite.SrcOver);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, height - 2)));
        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth(text);
        int textH = fm.getAscent();
        int x = Math.max(0, (width - textW) / 2);
        int y = Math.max(textH, Math.min(height - 2, (height + textH) / 2 - 1));
        g.setColor(textColor);
        g.drawString(text, x, y);
        g.dispose();
        return image;
    }

    private BufferedImage createStripeImage(int width, int height) {
        BufferedImage image = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setComposite(AlphaComposite.Src);
        for (int y = 0; y < image.getHeight(); y++) {
            boolean blackBand = y % 2 == 0;
            Color bandColor = blackBand ? Color.BLACK : Color.WHITE;
            g.setColor(new Color(bandColor.getRed(), bandColor.getGreen(), bandColor.getBlue(), 255));
            g.fillRect(0, y, image.getWidth(), 1);
        }
        g.dispose();
        return image;
    }

    private BufferedImage rotateImageByAngle(BufferedImage src, double angle) {
        if (Math.abs(angle) < 0.000001D) {
            return src;
        }
        double rad = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(rad));
        double cos = Math.abs(Math.cos(rad));
        int w = src.getWidth();
        int h = src.getHeight();
        int newW = Math.max(1, (int) Math.ceil(w * cos + h * sin));
        int newH = Math.max(1, (int) Math.ceil(h * cos + w * sin));
        BufferedImage rotated = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
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

    private void updateMarks(ProductionPiece piece, MarkAssets assets) {
        Map<String, String> marks = piece.getMarks();
        if (marks == null) {
            marks = new LinkedHashMap<>();
            piece.setMarks(marks);
        }
        marks.put("superWidthSpliceDarkMark", assets.darkMark);
        for (Map.Entry<SpliceEdge, EdgeAssets> entry : assets.edgeAssets.entrySet()) {
            EdgeAssets edgeAssets = entry.getValue();
            String suffix = entry.getKey().name().toLowerCase(Locale.ROOT);
            marks.put("superWidthSpliceStripe-" + suffix, edgeAssets.stripe);
            marks.put("superWidthSpliceWhiteText-" + suffix, edgeAssets.whiteText);
            marks.put("superWidthSpliceGrayText-" + suffix, edgeAssets.grayText);
        }
    }

    private void updateMaskImageFile(ProductionPiece piece, String maskUrl) {
        ImageFile maskFile = piece.getMaskImageFile();
        if (maskFile == null) {
            maskFile = new ImageFile();
            piece.setMaskImageFile(maskFile);
        }
        maskFile.setRawFile(maskUrl);
        FilePreview preview = maskFile.getFilePreview();
        if (preview == null) {
            preview = new FilePreview();
            maskFile.setFilePreview(preview);
        }
        preview.setRaw(maskUrl);
        preview.setPreview(maskUrl);
        preview.setThumbnail(maskUrl);
    }

    private String ensureProductionPieceMongoId(ProductionPiece piece) {
        if (StringUtils.isBlank(piece.getId())) {
            piece.setId(new ObjectId().toHexString());
        }
        return piece.getId();
    }

    private String ensureProductionPieceBusinessId(ProductionPiece piece) {
        if (StringUtils.isBlank(piece.getProductionPieceId())) {
            piece.setProductionPieceId(IdGenerator.generateId("PP"));
        }
        return piece.getProductionPieceId();
    }

    private String resolveManufacturerMetaId(OrderItem orderItem, ProductionPiece piece) {
        if (orderItem != null && StringUtils.isNotBlank(orderItem.getManufacturerId())) {
            return orderItem.getManufacturerId();
        }
        if (piece != null && StringUtils.isNotBlank(piece.getManufacturerId())) {
            return piece.getManufacturerId();
        }
        return "default";
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

    private boolean containsGroup(String svg) {
        return StringUtils.isNotBlank(svg) && SVG_GROUP_PATTERN.matcher(svg).find();
    }

    private int lastSvgCloseIndex(String svg) {
        Matcher matcher = SVG_CLOSE_PATTERN.matcher(svg);
        int closeIndex = -1;
        while (matcher.find()) {
            closeIndex = matcher.start();
        }
        return closeIndex;
    }

    private String normalizeRectsToPaths(String svgContent) {
        if (StringUtils.isBlank(svgContent)) {
            return svgContent;
        }
        Matcher matcher = SVG_RECT_PATTERN.matcher(svgContent);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String rectAttributes = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
            String path = convertRectToPath(matcher.group(), rectAttributes);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(path));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String convertRectToPath(String originalRect, String rectAttributes) {
        Double x = parseSvgNumber(attributeValue(rectAttributes, "x"), 0D);
        Double y = parseSvgNumber(attributeValue(rectAttributes, "y"), 0D);
        Double width = parseSvgNumber(attributeValue(rectAttributes, "width"), null);
        Double height = parseSvgNumber(attributeValue(rectAttributes, "height"), null);
        if (x == null || y == null || width == null || height == null) {
            return originalRect;
        }
        StringBuilder path = new StringBuilder("<path d=\"M")
                .append(format(x)).append(" ").append(format(y))
                .append(" H").append(format(x + width))
                .append(" V").append(format(y + height))
                .append(" H").append(format(x))
                .append(" Z\"");
        appendRectAttributesAsPathAttributes(path, rectAttributes);
        path.append("/>");
        return path.toString();
    }

    private void appendRectAttributesAsPathAttributes(StringBuilder path, String rectAttributes) {
        Matcher matcher = SVG_ATTRIBUTE_PATTERN.matcher(rectAttributes);
        while (matcher.find()) {
            String attributeName = matcher.group(1);
            if ("x".equalsIgnoreCase(attributeName) || "y".equalsIgnoreCase(attributeName)
                    || "width".equalsIgnoreCase(attributeName) || "height".equalsIgnoreCase(attributeName)) {
                continue;
            }
            path.append(matcher.group());
        }
    }

    private String attributeValue(String attributes, String name) {
        if (StringUtils.isBlank(attributes) || StringUtils.isBlank(name)) {
            return null;
        }
        Pattern pattern = Pattern.compile("\\s" + Pattern.quote(name) + "\\s*=\\s*([\"'])(.*?)\\1", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(attributes);
        return matcher.find() ? matcher.group(2) : null;
    }

    private Double parseSvgNumber(String value, Double defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim().replaceAll("(?i)(px|mm)$", ""));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String sourceName(String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        int queryIndex = url.indexOf('?');
        String clean = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
        int slashIndex = clean.lastIndexOf('/');
        return slashIndex >= 0 ? clean.substring(slashIndex + 1) : clean;
    }

    private String escapeAttr(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.000001D) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private boolean isZero(Integer value) {
        return value != null && value == 0;
    }

    private boolean isNonZero(Integer value) {
        return value != null && value != 0;
    }

    private Color createGrayColor(int blackPercent) {
        int black = Math.max(0, Math.min(100, blackPercent));
        int v = (int) Math.round(255 * (1 - black / 100.0));
        return new Color(v, v, v);
    }

    private enum SpliceEdge {
        TOP, RIGHT, BOTTOM, LEFT
    }

    private static class MarkAssets {
        private final String darkMark;
        private final Map<SpliceEdge, EdgeAssets> edgeAssets;
        private final double darkWidth;
        private final double darkHeight;

        private MarkAssets(String darkMark, Map<SpliceEdge, EdgeAssets> edgeAssets, double darkWidth, double darkHeight) {
            this.darkMark = darkMark;
            this.edgeAssets = edgeAssets;
            this.darkWidth = darkWidth;
            this.darkHeight = darkHeight;
        }
    }

    private static class EdgeAssets {
        private final String stripe;
        private final String whiteText;
        private final String grayText;
        private final double stripeWidth;
        private final double stripeHeight;
        private final double textWidth;
        private final double textHeight;

        private EdgeAssets(String stripe, String whiteText, String grayText,
                           double stripeWidth, double stripeHeight, double textWidth, double textHeight) {
            this.stripe = stripe;
            this.whiteText = whiteText;
            this.grayText = grayText;
            this.stripeWidth = stripeWidth;
            this.stripeHeight = stripeHeight;
            this.textWidth = textWidth;
            this.textHeight = textHeight;
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

        private Edge(PointD start, PointD end, PointD normal) {
            this.start = start;
            this.end = end;
            this.normal = normal;
        }
    }

    private static class Rect {
        private final double x;
        private final double y;
        private final double width;
        private final double height;

        private Rect(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}

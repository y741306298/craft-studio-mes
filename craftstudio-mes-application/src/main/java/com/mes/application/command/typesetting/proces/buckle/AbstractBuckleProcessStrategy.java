package com.mes.application.command.typesetting.proces.buckle;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.shared.utils.IdGenerator;
import com.piliofpala.craftstudio.shared.domain.file.vo.FilePreview;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 订单预处理阶段的“打扣”抽象策略。
 *
 * <p>职责边界：</p>
 * <ul>
 *     <li>统一完成打扣工艺节点匹配、mask SVG 读取、尺寸校验、扣点 SVG 追加、上传和工件字段回写。</li>
 *     <li>统一保留原 SVG 已有 {@code <g>} 分组，避免覆盖留白等前置预处理结果。</li>
 *     <li>实体策略只需要声明节点名称、扣点 key 前缀以及应该落点的边/角。</li>
 * </ul>
 *
 * <p>扩展方式：</p>
 * <ol>
 *     <li>新增实体策略继承本类并注册为 Spring Bean。</li>
 *     <li>在 {@link #nodeName()} 中返回工艺节点名称。</li>
 *     <li>在 {@link #buildMarkPoints(double, double)} 中返回该打扣规格的扣点中心坐标。</li>
 * </ol>
 */
@Slf4j
public abstract class AbstractBuckleProcessStrategy {
    protected static final String MARK_IMG = "https://craftstudio-mes-prod.oss-cn-hangzhou.aliyuncs.com/basetag/point.png";
    protected static final String MARK_SOURCE_NAME = "point.png";
    protected static final double MARK_SIZE_MM = 8D;
    protected static final double EDGE_OFFSET_MM = 25D;
    protected static final double MAX_BUCKLE_SPACING_MM = 300D;

    private static final String MARK_PATH_FILL = "#000000";
    private static final Pattern SVG_WIDTH_PATTERN = Pattern.compile("width\\s*=\\s*[\"']\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:px|mm)?\\s*[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_HEIGHT_PATTERN = Pattern.compile("height\\s*=\\s*[\"']\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:px|mm)?\\s*[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_OPEN_PATTERN = Pattern.compile("<svg\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_CLOSE_PATTERN = Pattern.compile("</svg\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_GROUP_PATTERN = Pattern.compile("<g\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_RECT_PATTERN = Pattern.compile("<rect\\b([^>]*)\\s*/>|<rect\\b([^>]*)>\\s*</rect\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_ATTRIBUTE_PATTERN = Pattern.compile("\\s+([A-Za-z_:][-A-Za-z0-9_:.]*)\\s*=\\s*([\"']).*?\\2", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_NUMBER_PATTERN = Pattern.compile("[-+]?[0-9]+(?:\\.[0-9]+)?");

    private final RestTemplate restTemplate;
    private final OssTagUploadService ossTagUploadService;

    protected AbstractBuckleProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        this.restTemplate = restTemplate;
        this.ossTagUploadService = ossTagUploadService;
    }

    /**
     * 判断当前上下文是否命中该打扣实体策略。
     *
     * @param context 打扣处理上下文
     * @return {@code true} 表示当前实体策略可处理该上下文
     */
    public boolean matches(BuckleProcessContext context) {
        if (context == null || context.getProcedureFlow() == null) {
            return false;
        }
        return hasNode(context.getProcedureFlow(), nodeName());
    }

    /**
     * 执行打扣策略。
     *
     * @param context 打扣处理上下文
     */
    public void process(BuckleProcessContext context) {
        ProductionPiece piece = context == null ? null : context.getProductionPiece();
        if (piece == null || piece.getMaskImageFile() == null || StringUtils.isBlank(piece.getMaskImageFile().getRawFile())) {
            return;
        }
        String originalMaskUrl = piece.getMaskImageFile().getRawFile();
        String originalSvg = resolveSvg(originalMaskUrl);
        if (StringUtils.isBlank(originalSvg)) {
            return;
        }
        double width = resolveDimension(originalSvg, SVG_WIDTH_PATTERN, piece.getWidth());
        double height = resolveDimension(originalSvg, SVG_HEIGHT_PATTERN, piece.getHeight());
        if (width < EDGE_OFFSET_MM * 2 || height < EDGE_OFFSET_MM * 2) {
            log.info("{}预处理跳过尺寸不足工件: productionPieceId={}, width={}, height={}", nodeName(), piece.getProductionPieceId(), width, height);
            return;
        }
        List<BuckleMarkPoint> markPoints = buildMarkPoints(width, height);
        if (markPoints.isEmpty()) {
            return;
        }
        if (originalSvg.contains(markKeyPrefix() + "-")) {
            updateMarks(piece, markPoints);
            return;
        }
        ensureProductionPieceMongoId(piece);
        String businessId = ensureProductionPieceBusinessId(piece);
        String originalContentImg = resolveOriginalContentImg(piece, originalMaskUrl);
        String expandedSvg = appendBuckleMarks(originalSvg, piece, originalContentImg, markPoints);
        OrderItem orderItem = context.getOrderItem();
        String manufacturerMetaId = resolveManufacturerMetaId(orderItem, piece);
        String orderItemId = orderItem == null || StringUtils.isBlank(orderItem.getOrderItemId()) ? "default" : orderItem.getOrderItemId();
        String uploadPath = "mask/" + manufacturerMetaId + "/" + orderItemId + "/buckle/";
        String newMaskUrl = ossTagUploadService.uploadTagSvg(businessId, expandedSvg.getBytes(StandardCharsets.UTF_8), uploadPath);
        updateMaskImageFile(piece, newMaskUrl);
        updateMarks(piece, markPoints);
        log.info("{}预处理完成: productionPieceId={}, mask={}", nodeName(), piece.getProductionPieceId(), newMaskUrl);
    }

    /** @return 当前策略精确匹配的工艺节点名称。 */
    protected abstract String nodeName();

    /** @return 当前策略写入 SVG 与 marks map 使用的 key 前缀。 */
    protected abstract String markKeyPrefix();

    /**
     * 构建当前策略的所有扣点中心坐标。
     *
     * @param width mask SVG 宽度，单位 mm
     * @param height mask SVG 高度，单位 mm
     * @return 扣点列表
     */
    protected abstract List<BuckleMarkPoint> buildMarkPoints(double width, double height);

    protected List<BuckleMarkPoint> buildCornerMarkPoints(double width, double height) {
        List<BuckleMarkPoint> points = new ArrayList<>();
        points.add(new BuckleMarkPoint("lt", EDGE_OFFSET_MM, EDGE_OFFSET_MM));
        points.add(new BuckleMarkPoint("rt", width - EDGE_OFFSET_MM, EDGE_OFFSET_MM));
        points.add(new BuckleMarkPoint("rb", width - EDGE_OFFSET_MM, height - EDGE_OFFSET_MM));
        points.add(new BuckleMarkPoint("lb", EDGE_OFFSET_MM, height - EDGE_OFFSET_MM));
        return points;
    }

    protected List<BuckleMarkPoint> buildEdgeMarkPoints(double width, double height, List<BuckleEdge> edges) {
        Map<String, BuckleMarkPoint> points = new LinkedHashMap<>();
        for (BuckleEdge edge : edges) {
            addEdgeMarkPoints(points, edge, width, height);
        }
        return new ArrayList<>(points.values());
    }

    private void addEdgeMarkPoints(Map<String, BuckleMarkPoint> points, BuckleEdge edge, double width, double height) {
        double startX;
        double startY;
        double endX;
        double endY;
        switch (edge) {
            case TOP:
                startX = EDGE_OFFSET_MM;
                startY = EDGE_OFFSET_MM;
                endX = width - EDGE_OFFSET_MM;
                endY = EDGE_OFFSET_MM;
                break;
            case RIGHT:
                startX = width - EDGE_OFFSET_MM;
                startY = EDGE_OFFSET_MM;
                endX = width - EDGE_OFFSET_MM;
                endY = height - EDGE_OFFSET_MM;
                break;
            case BOTTOM:
                startX = width - EDGE_OFFSET_MM;
                startY = height - EDGE_OFFSET_MM;
                endX = EDGE_OFFSET_MM;
                endY = height - EDGE_OFFSET_MM;
                break;
            case LEFT:
                startX = EDGE_OFFSET_MM;
                startY = height - EDGE_OFFSET_MM;
                endX = EDGE_OFFSET_MM;
                endY = EDGE_OFFSET_MM;
                break;
            default:
                return;
        }
        addPoint(points, edge.name().toLowerCase() + "-0", startX, startY);
        double edgeLength = distance(startX, startY, endX, endY);
        if (edgeLength > MAX_BUCKLE_SPACING_MM) {
            int segmentCount = (int) Math.ceil(edgeLength / MAX_BUCKLE_SPACING_MM);
            double spacing = edgeLength / segmentCount;
            double unitX = (endX - startX) / edgeLength;
            double unitY = (endY - startY) / edgeLength;
            for (int i = 1; i < segmentCount; i++) {
                addPoint(points, edge.name().toLowerCase() + "-" + i, startX + unitX * spacing * i, startY + unitY * spacing * i);
            }
        }
        addPoint(points, edge.name().toLowerCase() + "-end", endX, endY);
    }

    private void addPoint(Map<String, BuckleMarkPoint> points, String suffix, double centerX, double centerY) {
        String coordinateKey = format(centerX) + "," + format(centerY);
        points.putIfAbsent(coordinateKey, new BuckleMarkPoint(suffix, centerX, centerY));
    }

    private boolean hasNode(ProcedureFlow procedureFlow, String nodeName) {
        return procedureFlow.getNodes() != null && procedureFlow.getNodes().stream()
                .anyMatch(node -> node != null && nodeName.equals(node.getNodeName()));
    }

    private String resolveSvg(String svgRefOrContent) {
        if (StringUtils.isBlank(svgRefOrContent)) {
            return null;
        }
        String trimmed = svgRefOrContent.trim();
        if (trimmed.startsWith("<svg") || trimmed.startsWith("<?xml")) {
            return trimmed;
        }
        try {
            return restTemplate.getForObject(trimmed, String.class);
        } catch (Exception e) {
            log.warn("下载{} mask SVG 失败: {}, error={}", nodeName(), svgRefOrContent, e.getMessage());
            return null;
        }
    }

    private double resolveDimension(String svg, Pattern pattern, Double fallback) {
        Matcher matcher = pattern.matcher(svg);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (Exception ignored) {
                // fallback below
            }
        }
        return fallback == null ? 0D : fallback;
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

    private String appendBuckleMarks(String originalSvg, ProductionPiece piece, String originalContentImg,
                                     List<BuckleMarkPoint> markPoints) {
        int closeIndex = lastSvgCloseIndex(originalSvg);
        if (closeIndex < 0) {
            return originalSvg;
        }
        String pieceId = StringUtils.isNotBlank(piece.getId()) ? piece.getId() : piece.getProductionPieceId();
        if (StringUtils.isBlank(pieceId)) {
            pieceId = "unknown";
        }
        String baseSvg = containsGroup(originalSvg) ? originalSvg : rebuildSvgWithOriginalGroup(originalSvg, pieceId, originalContentImg);
        closeIndex = lastSvgCloseIndex(baseSvg);
        if (closeIndex < 0) {
            return baseSvg;
        }
        String marksSvg = buildMarksSvg(pieceId, markPoints);
        return baseSvg.substring(0, closeIndex) + marksSvg + baseSvg.substring(closeIndex);
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

    private String rebuildSvgWithOriginalGroup(String originalSvg, String pieceId, String originalContentImg) {
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
        return prefix + "\n" + buildOriginalContentGroup(pieceId, originalContentImg, innerSvg) + suffix;
    }

    private String buildOriginalContentGroup(String pieceId, String originalContentImg, String innerSvg) {
        String groupId = StringUtils.isBlank(pieceId) ? "original-mask" : pieceId;
        return "<g id=\"" + escapeAttr(groupId)
                + "\" img=\"" + escapeAttr(originalContentImg)
                + "\" data-source-name=\"" + escapeAttr(sourceName(originalContentImg))
                + "\" data-forme=\"true\" data-rotation=\"0\">\n"
                + normalizeRectsToPaths(innerSvg) + "\n"
                + "</g>\n";
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
        Matcher matcher = SVG_ATTRIBUTE_PATTERN.matcher(attributes);
        while (matcher.find()) {
            if (name.equalsIgnoreCase(matcher.group(1))) {
                String attribute = matcher.group();
                int equalsIndex = attribute.indexOf('=');
                if (equalsIndex < 0) {
                    return null;
                }
                String value = attribute.substring(equalsIndex + 1).trim();
                if (value.length() >= 2 && (value.startsWith("\"") || value.startsWith("'"))) {
                    return value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        return null;
    }

    private Double parseSvgNumber(String value, Double fallback) {
        if (StringUtils.isBlank(value)) {
            return fallback;
        }
        Matcher matcher = SVG_NUMBER_PATTERN.matcher(value.trim());
        if (!matcher.find()) {
            return fallback;
        }
        try {
            return Double.parseDouble(matcher.group());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String resolveOriginalContentImg(ProductionPiece piece, String originalMaskUrl) {
        String productImg = resolveImageFileRaw(piece == null ? null : piece.getProductImageFile());
        if (StringUtils.isNotBlank(productImg)) {
            return productImg;
        }
        return isInlineSvg(originalMaskUrl) ? "" : originalMaskUrl;
    }

    private String resolveImageFileRaw(ImageFile imageFile) {
        if (imageFile == null) {
            return null;
        }
        if (StringUtils.isNotBlank(imageFile.getRawFile())) {
            return imageFile.getRawFile();
        }
        FilePreview preview = imageFile.getFilePreview();
        if (preview != null && StringUtils.isNotBlank(preview.getRaw())) {
            return preview.getRaw();
        }
        return null;
    }

    private boolean isInlineSvg(String svgRef) {
        if (StringUtils.isBlank(svgRef)) {
            return false;
        }
        String trimmed = svgRef.trim();
        return trimmed.startsWith("<svg") || trimmed.startsWith("<?xml");
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

    private String buildMarksSvg(String pieceId, List<BuckleMarkPoint> markPoints) {
        StringBuilder builder = new StringBuilder("\n");
        for (BuckleMarkPoint point : markPoints) {
            appendPointGroup(builder, pieceId, point);
        }
        return builder.toString();
    }

    private void appendPointGroup(StringBuilder builder, String pieceId, BuckleMarkPoint point) {
        double x = point.getCenterX() - MARK_SIZE_MM / 2D;
        double y = point.getCenterY() - MARK_SIZE_MM / 2D;
        builder.append("<g id=\"").append(markKeyPrefix()).append("-").append(point.getSuffix()).append("-").append(escapeAttr(pieceId))
                .append("\" img=\"").append(escapeAttr(MARK_IMG))
                .append("\" data-source-name=\"").append(MARK_SOURCE_NAME)
                .append("\" data-forme=\"false\" data-rotation=\"0\" transform=\"translate(").append(format(x)).append(" ").append(format(y)).append(")\">\n")
                .append("<path d=\"").append(buildPointPath())
                .append("\" fill=\"").append(MARK_PATH_FILL).append("\"/>\n")
                .append("</g>\n");
    }

    private String buildPointPath() {
        double radius = MARK_SIZE_MM / 2D;
        double c = radius * 0.5522847498307936D;
        return "M" + format(radius) + " 0"
                + " C" + format(radius + c) + " 0 " + format(MARK_SIZE_MM) + " " + format(radius - c) + " " + format(MARK_SIZE_MM) + " " + format(radius)
                + " C" + format(MARK_SIZE_MM) + " " + format(radius + c) + " " + format(radius + c) + " " + format(MARK_SIZE_MM) + " " + format(radius) + " " + format(MARK_SIZE_MM)
                + " C" + format(radius - c) + " " + format(MARK_SIZE_MM) + " 0 " + format(radius + c) + " 0 " + format(radius)
                + " C0 " + format(radius - c) + " " + format(radius - c) + " 0 " + format(radius) + " 0 Z";
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

    private void updateMarks(ProductionPiece piece, List<BuckleMarkPoint> markPoints) {
        Map<String, String> marks = piece.getMarks();
        if (marks == null) {
            marks = new LinkedHashMap<>();
            piece.setMarks(marks);
        }
        for (BuckleMarkPoint point : markPoints) {
            marks.put(markKeyPrefix() + "-" + point.getSuffix(), MARK_IMG);
        }
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

    private double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.000001D) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(java.util.Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
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
}

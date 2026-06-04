package com.mes.application.command.typesetting.proces.buckle;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.shared.utils.IdGenerator;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.piliofpala.craftstudio.shared.domain.file.vo.FilePreview;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 订单预处理阶段的“四角打扣”扣点处理服务。
 *
 * <p>该服务与留白预处理保持同一处理时机：在生产工件创建后、持久化前同步处理
 * {@link ProductionPiece#getMaskImageFile()} 指向的 mask SVG；原 SVG 已有 {@code <g>} 时只在根 {@code <svg>}
 * 关闭标签前追加四个扣点分组，不重写原有分组，避免覆盖留白等已提前写入的 {@code <g>}；原 SVG
 * 没有 {@code <g>} 时先重构为分组结构，再把扣点追加在后面。后续排版/刀版流程直接消费已经带扣点的 productionPiece。</p>
 */
@Slf4j
@Service
public class FourCornerBuckleProcessService {
    private static final String NODE_NAME = "四角打扣";
    private static final String MARK_IMG = "https://craftstudio-mes-prod.oss-cn-hangzhou.aliyuncs.com/basetag/point.png";
    private static final String MARK_KEY_PREFIX = "four-corner-buckle-point";
    private static final String MARK_SOURCE_NAME = "point.png";
    private static final double MARK_SIZE_MM = 8D;
    private static final double EDGE_OFFSET_MM = 25D;
    private static final Pattern SVG_WIDTH_PATTERN = Pattern.compile("width\\s*=\\s*[\"']\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:px|mm)?\\s*[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_HEIGHT_PATTERN = Pattern.compile("height\\s*=\\s*[\"']\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:px|mm)?\\s*[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_OPEN_PATTERN = Pattern.compile("<svg\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_CLOSE_PATTERN = Pattern.compile("</svg\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_GROUP_PATTERN = Pattern.compile("<g\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_RECT_PATTERN = Pattern.compile("<rect\\b([^>]*)\\s*/>|<rect\\b([^>]*)>\\s*</rect\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_ATTRIBUTE_PATTERN = Pattern.compile("\\s+([A-Za-z_:][-A-Za-z0-9_:.]*)\\s*=\\s*([\"']).*?\\2", Pattern.CASE_INSENSITIVE);

    private final RestTemplate restTemplate;
    private final OssTagUploadService ossTagUploadService;

    public FourCornerBuckleProcessService(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        this.restTemplate = restTemplate;
        this.ossTagUploadService = ossTagUploadService;
    }

    /**
     * 如果工艺流程包含“四角打扣”，则为当前生产工件 mask SVG 写入四个扣点。
     */
    public void process(OrderItem orderItem, ProcedureFlow procedureFlow, ProductionPiece piece) {
        if (!containsFourCornerBuckleNode(procedureFlow) || piece == null || piece.getMaskImageFile() == null
                || StringUtils.isBlank(piece.getMaskImageFile().getRawFile())) {
            return;
        }
        String originalMaskUrl = piece.getMaskImageFile().getRawFile();
        String originalSvg = resolveSvg(originalMaskUrl);
        if (StringUtils.isBlank(originalSvg)) {
            return;
        }
        if (originalSvg.contains("four-corner-buckle-")) {
            updateMarks(piece);
            return;
        }
        double width = resolveDimension(originalSvg, SVG_WIDTH_PATTERN, piece.getWidth());
        double height = resolveDimension(originalSvg, SVG_HEIGHT_PATTERN, piece.getHeight());
        if (width < EDGE_OFFSET_MM * 2 || height < EDGE_OFFSET_MM * 2) {
            log.info("四角打扣预处理跳过尺寸不足工件: productionPieceId={}, width={}, height={}",
                    piece.getProductionPieceId(), width, height);
            return;
        }
        ensureProductionPieceMongoId(piece);
        String businessId = ensureProductionPieceBusinessId(piece);
        String originalContentImg = resolveOriginalContentImg(piece, originalMaskUrl);
        String expandedSvg = appendBuckleMarks(originalSvg, piece, originalContentImg, width, height);
        String manufacturerMetaId = resolveManufacturerMetaId(orderItem, piece);
        String orderItemId = orderItem == null || StringUtils.isBlank(orderItem.getOrderItemId()) ? "default" : orderItem.getOrderItemId();
        String uploadPath = "mask/" + manufacturerMetaId + "/" + orderItemId + "/buckle/";
        String newMaskUrl = ossTagUploadService.uploadTagSvg(businessId, expandedSvg.getBytes(StandardCharsets.UTF_8), uploadPath);
        updateMaskImageFile(piece, newMaskUrl);
        updateMarks(piece);
        log.info("四角打扣预处理完成: productionPieceId={}, mask={}", piece.getProductionPieceId(), newMaskUrl);
    }

    private boolean containsFourCornerBuckleNode(ProcedureFlow procedureFlow) {
        if (procedureFlow == null || procedureFlow.getNodes() == null) {
            return false;
        }
        for (ProcedureFlowNode node : procedureFlow.getNodes()) {
            if (node != null && NODE_NAME.equals(node.getNodeName())) {
                return true;
            }
        }
        return false;
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
            log.warn("下载四角打扣 mask SVG 失败: {}, error={}", svgRefOrContent, e.getMessage());
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

    /**
     * 拼接四个扣点 {@code <g>}。
     *
     * <p>注意：原 SVG 已存在 {@code <g>} 时必须只追加新的扣点分组，不能重建或包裹原有 SVG 内容。
     * 同一个工件可能先执行留白再执行四角打扣，重写原有 {@code <g>} 会导致留白分组丢失。
     * 原 SVG 不存在 {@code <g>} 时先重构为分组结构，再把扣点追加在后面。</p>
     */
    private String appendBuckleMarks(String originalSvg, ProductionPiece piece, String originalContentImg, double width, double height) {
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
        String marksSvg = buildMarksSvg(pieceId, width, height);
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
        Matcher matcher = Pattern.compile("[-+]?[0-9]+(?:\\.[0-9]+)?").matcher(value.trim());
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

    private String buildMarksSvg(String pieceId, double width, double height) {
        StringBuilder builder = new StringBuilder("\n");
        appendPointGroup(builder, pieceId, "lt", EDGE_OFFSET_MM, EDGE_OFFSET_MM);
        appendPointGroup(builder, pieceId, "rt", width - EDGE_OFFSET_MM, EDGE_OFFSET_MM);
        appendPointGroup(builder, pieceId, "rb", width - EDGE_OFFSET_MM, height - EDGE_OFFSET_MM);
        appendPointGroup(builder, pieceId, "lb", EDGE_OFFSET_MM, height - EDGE_OFFSET_MM);
        return builder.toString();
    }

    private void appendPointGroup(StringBuilder builder, String pieceId, String suffix, double centerX, double centerY) {
        double x = centerX - MARK_SIZE_MM / 2D;
        double y = centerY - MARK_SIZE_MM / 2D;
        builder.append("<g id=\"").append(MARK_KEY_PREFIX).append("-").append(suffix).append("-").append(escapeAttr(pieceId))
                .append("\" img=\"").append(escapeAttr(MARK_IMG))
                .append("\" data-source-name=\"").append(MARK_SOURCE_NAME)
                .append("\" data-forme=\"false\" data-rotation=\"0\" transform=\"translate(").append(format(x)).append(" ").append(format(y)).append(")\">\n")
                .append("<image href=\"").append(escapeAttr(MARK_IMG))
                .append("\" x=\"0\" y=\"0\" width=\"").append(format(MARK_SIZE_MM))
                .append("\" height=\"").append(format(MARK_SIZE_MM))
                .append("\" preserveAspectRatio=\"none\"/>\n")
                .append("</g>\n");
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

    private void updateMarks(ProductionPiece piece) {
        Map<String, String> marks = piece.getMarks();
        if (marks == null) {
            marks = new LinkedHashMap<>();
            piece.setMarks(marks);
        }
        marks.put(MARK_KEY_PREFIX + "-lt", MARK_IMG);
        marks.put(MARK_KEY_PREFIX + "-rt", MARK_IMG);
        marks.put(MARK_KEY_PREFIX + "-rb", MARK_IMG);
        marks.put(MARK_KEY_PREFIX + "-lb", MARK_IMG);
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

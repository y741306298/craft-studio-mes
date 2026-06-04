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
 * {@link ProductionPiece#getMaskImageFile()} 指向的 mask SVG。已有 {@code <g>} 时只在根 {@code <svg>} 关闭标签前追加四个扣点分组；
 * 没有 {@code <g>} 时先重写 SVG，把原始图形放入基础分组，再把扣点分组放在后面，避免覆盖留白等已提前写入的 {@code <g>}。</p>
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
    private static final Pattern SVG_WIDTH_PATTERN = Pattern.compile("width\\s*=\\s*[\"']\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:px)?\\s*[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_HEIGHT_PATTERN = Pattern.compile("height\\s*=\\s*[\"']\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:px)?\\s*[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern BUCKLE_SVG_GROUP_PATTERN = Pattern.compile("<g\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BUCKLE_SVG_CLOSE_PATTERN = Pattern.compile("</svg\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern BUCKLE_SVG_INNER_PATTERN = Pattern.compile("<svg\\b[^>]*>([\\s\\S]*?)</svg>", Pattern.CASE_INSENSITIVE);

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
        String expandedSvg = appendBuckleMarks(originalSvg, originalMaskUrl, piece, width, height);
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
     * 写入四个扣点 {@code <g>}。
     *
     * <p>已有 {@code <g>} 时只追加新的扣点分组到根节点末尾；没有 {@code <g>} 时重写 SVG，
     * 先把原始图形放入基础分组，再追加扣点分组，确保扣点在后面且后续流程能按 {@code <g>} 增量处理。</p>
     */
    private String appendBuckleMarks(String originalSvg, String originalMaskUrl, ProductionPiece piece, double width, double height) {
        String pieceId = StringUtils.isNotBlank(piece.getId()) ? piece.getId() : piece.getProductionPieceId();
        if (StringUtils.isBlank(pieceId)) {
            pieceId = "unknown";
        }
        String marksSvg = buildMarksSvg(pieceId, width, height);
        if (!hasGroup(originalSvg)) {
            return rebuildBuckleSvg(originalSvg, originalMaskUrl, piece, pieceId, width, height, marksSvg);
        }
        int closeIndex = lastSvgCloseIndex(originalSvg);
        if (closeIndex < 0) {
            return originalSvg;
        }
        return originalSvg.substring(0, closeIndex) + marksSvg + originalSvg.substring(closeIndex);
    }

    private boolean hasGroup(String svg) {
        return BUCKLE_SVG_GROUP_PATTERN.matcher(svg).find();
    }

    private int lastSvgCloseIndex(String svg) {
        Matcher matcher = BUCKLE_SVG_CLOSE_PATTERN.matcher(svg);
        int closeIndex = -1;
        while (matcher.find()) {
            closeIndex = matcher.start();
        }
        return closeIndex;
    }

    private String rebuildBuckleSvg(String originalSvg,
                                    String originalMaskUrl,
                                    ProductionPiece piece,
                                    String pieceId,
                                    double width,
                                    double height,
                                    String marksSvg) {
        String inner = extractInnerSvg(originalSvg);
        String productImg = piece.getProductImageFile() == null ? "" : piece.getProductImageFile().getRawFile();
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + format(width) + "\" height=\"" + format(height)
                + "\" viewBox=\"0 0 " + format(width) + " " + format(height) + "\" version=\"1.1\" require-plt=\"true\">\n"
                + "<g id=\"" + escapeAttr(pieceId) + "\" img=\"" + escapeAttr(productImg)
                + "\" data-source-name=\"" + escapeAttr(sourceName(originalMaskUrl)) + "\" data-forme=\"false\" data-rotation=\"0\">\n"
                + inner + "\n"
                + "</g>"
                + marksSvg
                + "</svg>";
    }

    private String extractInnerSvg(String svg) {
        String withoutXml = svg.replaceFirst("(?is)^\\s*<\\?xml[^>]*>\\s*", "");
        Matcher matcher = BUCKLE_SVG_INNER_PATTERN.matcher(withoutXml);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return withoutXml.trim();
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

    private String sourceName(String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        int queryIndex = url.indexOf('?');
        String clean = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
        int slashIndex = clean.lastIndexOf('/');
        return slashIndex >= 0 ? clean.substring(slashIndex + 1) : clean;
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

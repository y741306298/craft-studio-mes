package com.mes.application.command.typesetting.proces.splice;

import com.mes.application.command.graphics.RemoteGraphics;
import com.mes.application.command.graphics.RemotePieceGraphics;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.Blood;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.shared.utils.IdGenerator;
import com.piliofpala.craftstudio.shared.domain.file.vo.FilePreview;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 拼接回调预处理服务。
 *
 * <p>在蒙版算法 callback 创建 {@link ProductionPiece} 后、生产工件落库前运行，直接把超幅/背胶/覆板/喷绘拼接的
 * 出血边/被出血边标识写入当前工件 mask SVG，并回写 {@code productionPiece.marks}。这样后续
 * toLayout 会像留白/打扣预处理后的工件一样，把该工件作为带 marks 的 forme 元素提交排版。</p>
 */
@Slf4j
@Service
public class SuperWidthSpliceProcessService {
    private static final String SUPER_WIDTH_SPLICE_NODE_NAME = "超幅拼接";
    private static final String ADHESIVE_SPLICE_NODE_NAME = "背胶拼接";
    private static final String PHOTO_SPLICE_NODE_NAME = "写真拼接";
    private static final String BOARD_COVER_SPLICE_NODE_NAME = "覆板拼接";
    private static final String INKJET_SPLICE_NODE_NAME = "喷绘拼接";
    private static final String SEAMLESS_SPLICE_NODE_NAME = "无痕拼接";
    private static final String PANEL_SPLICE_NODE_NAME = "板材拼接";
    private static final Pattern SVG_OPEN_PATTERN = Pattern.compile("<svg\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_CLOSE_PATTERN = Pattern.compile("</svg\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_GROUP_PATTERN = Pattern.compile("<g\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_RECT_PATTERN = Pattern.compile("<rect\\b([^>]*)\\s*/>|<rect\\b([^>]*)>\\s*</rect\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_ATTRIBUTE_PATTERN = Pattern.compile("\\s+([A-Za-z_:][-A-Za-z0-9_:.]*)\\s*=\\s*([\"']).*?\\2", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_PATH_PATTERN = Pattern.compile("<path\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_PATH_TOKEN_PATTERN = Pattern.compile("[AaCcHhLlMmQqSsTtVvZz]|[-+]?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?");
    private static final Pattern MAX_SEQ_PATTERN = Pattern.compile("#\\s*\\d+-(\\d+)");
    private static final String MARK_REQUIRE_PLT_ATTR = " require-plt=\"false\"";
    private static final SpliceProcessConfig SUPER_WIDTH_SPLICE_CONFIG = new SpliceProcessConfig(
            SUPER_WIDTH_SPLICE_NODE_NAME, "super-width-splice", 30D, true);
    private static final SpliceProcessConfig ADHESIVE_SPLICE_CONFIG = new SpliceProcessConfig(
            ADHESIVE_SPLICE_NODE_NAME, "adhesive-splice", 20D, true);
    private static final SpliceProcessConfig PHOTO_SPLICE_CONFIG = new SpliceProcessConfig(
            PHOTO_SPLICE_NODE_NAME, "photo-splice", 0D, false);
    private static final SpliceProcessConfig BOARD_COVER_SPLICE_CONFIG = new SpliceProcessConfig(
            BOARD_COVER_SPLICE_NODE_NAME, "board-cover-splice", 20D, true);
    private static final SpliceProcessConfig INKJET_SPLICE_CONFIG = new SpliceProcessConfig(
            INKJET_SPLICE_NODE_NAME, "inkjet-splice", 30D, true);
    private static final SpliceProcessConfig SEAMLESS_SPLICE_CONFIG = new SpliceProcessConfig(
            SEAMLESS_SPLICE_NODE_NAME, "seamless-splice", 0D, false);
    private static final SpliceProcessConfig PANEL_SPLICE_CONFIG = new SpliceProcessConfig(
            PANEL_SPLICE_NODE_NAME, "panel-splice", 0D, false);
    private static final SpliceProcessConfig[] SPLICE_PROCESS_CONFIGS = {
            SUPER_WIDTH_SPLICE_CONFIG,
            ADHESIVE_SPLICE_CONFIG,
            PHOTO_SPLICE_CONFIG,
            BOARD_COVER_SPLICE_CONFIG,
            INKJET_SPLICE_CONFIG,
            SEAMLESS_SPLICE_CONFIG,
            PANEL_SPLICE_CONFIG
    };


    public void process(OrderItem orderItem, ProcedureFlow procedureFlow, ProductionPiece piece, Blood firstSeqBlood) {
        SpliceProcessConfig processConfig = resolveSpliceProcessConfig(procedureFlow, piece);
        if (orderItem == null || processConfig == null) {
            return;
        }
        if (piece.getMaskImageFile() == null || StringUtils.isBlank(piece.getMaskImageFile().getRawFile())) {
            return;
        }
        double width = piece.getWidth() == null ? 0D : piece.getWidth();
        double height = piece.getHeight() == null ? 0D : piece.getHeight();
        if (width <= 0D || height <= 0D) {
            return;
        }
        String originalMaskUrl = piece.getMaskImageFile().getRawFile();
        String originalSvg = resolveSvg(originalMaskUrl);
        if (StringUtils.isBlank(originalSvg)) throw new IllegalStateException("下载拼接 mask SVG 失败");
        boolean existingMarks = containsSpliceMarks(originalSvg);

        String pieceMongoId = ensureProductionPieceMongoId(piece);
        ensureProductionPieceBusinessId(piece);
        SpliceEdges spliceEdges = resolveSpliceEdges(procedureFlow, processConfig, piece, firstSeqBlood);
        if (!existingMarks && spliceEdges.bleedEdges.isEmpty() && spliceEdges.coveredEdges.isEmpty()) return;

        MarkAssets planned = markAssets(piece.getGroup(), java.util.function.Function.identity());
        List<Map<String, Object>> marks = existingMarks ? List.of() : buildMarks(processConfig, pieceMongoId, width, height,
                spliceEdges.bleedEdges, spliceEdges.coveredEdges, planned, resolveShapeEdgeSpans(originalSvg, width, height));
        RemotePieceGraphics result = RemoteGraphics.splicePiece(RemoteGraphics.params("originalSvg", originalSvg,
                "pieceMongoId", pieceMongoId, "originalContentImg", resolveOriginalContentImg(piece, originalMaskUrl),
                "groupText", piece.getGroup(), "marks", marks));
        // Validate all asset keys before changing either the mask or the production-piece metadata.
        MarkAssets completed = markAssets(piece.getGroup(), result::asset);
        if (!existingMarks) updateMaskImageFile(piece, result.maskUrl());
        updateMarks(piece, completed);
        String newMaskUrl = piece.getMaskImageFile().getRawFile();
        log.info("{}预处理完成: productionPieceId={}, mask={}", processConfig.nodeName, piece.getProductionPieceId(), newMaskUrl);
    }


    /**
     * 拼接标识命中条件：必须同时存在已支持的拼接工艺，并且当前零件带有 callback 回写的 seq / group。
     */
    private SpliceProcessConfig resolveSpliceProcessConfig(ProcedureFlow procedureFlow, ProductionPiece piece) {
        if (procedureFlow == null || procedureFlow.getNodes() == null || piece == null
                || piece.getSeq() == null || StringUtils.isBlank(piece.getGroup())) {
            return null;
        }
        // 多个拼接工艺同时存在时，只使用工艺流中最后一个拼接策略生成 callback 标识。
        return procedureFlow.getNodes().stream()
                .filter(node -> node != null)
                .filter(node -> resolveConfigByNodeName(node.getNodeName()) != null)
                .max(Comparator
                        .comparingInt((ProcedureFlowNode node) -> node.getNodeOrder() == null ? Integer.MIN_VALUE : node.getNodeOrder())
                        .thenComparingInt(procedureFlow.getNodes()::indexOf))
                .map(node -> resolveConfigByNodeName(node.getNodeName()))
                .orElse(null);
    }

    private SpliceProcessConfig resolveConfigByNodeName(String nodeName) {
        if (StringUtils.isBlank(nodeName)) {
            return null;
        }
        for (SpliceProcessConfig config : SPLICE_PROCESS_CONFIGS) {
            if (config.nodeName.equals(nodeName)) {
                return config;
            }
        }
        return null;
    }

    private boolean containsSpliceMarks(String svg) {
        if (StringUtils.isBlank(svg)) {
            return false;
        }
        for (SpliceProcessConfig config : SPLICE_PROCESS_CONFIGS) {
            if (svg.contains(config.markPrefix + "-")) {
                return true;
            }
        }
        return false;
    }

    private MarkAssets markAssets(String groupText, java.util.function.Function<String, String> reference) {
        double textWidthMm = Math.max(24D, groupText.length() * 8D);
        Map<SpliceEdge, EdgeAssets> edgeAssets = new EnumMap<>(SpliceEdge.class);
        for (SpliceEdge edge : SpliceEdge.values()) {
            boolean vertical = edge == SpliceEdge.LEFT || edge == SpliceEdge.RIGHT;
            String orientation = vertical ? "vertical" : "horizontal";
            edgeAssets.put(edge, new EdgeAssets(reference.apply("stripe-" + orientation),
                    reference.apply("yellow-" + orientation), reference.apply("gray-" + orientation),
                    vertical ? 1D : 6D, vertical ? 6D : 1D,
                    vertical ? 8D : textWidthMm, vertical ? textWidthMm : 8D));
        }
        return new MarkAssets(reference.apply("dark-horizontal"), reference.apply("dark-vertical"), edgeAssets, 6D, 1D, 1D, 6D);
    }

    private List<Map<String, Object>> buildMarks(SpliceProcessConfig processConfig,
                                 String pieceMongoId,
                                 double width,
                                 double height,
                                 Set<SpliceEdge> bleedEdges,
                                 Set<SpliceEdge> coveredEdges,
                                 MarkAssets assets,
                                 ShapeEdgeSpans shapeEdgeSpans) {
        List<Map<String, Object>> marks = new ArrayList<>();
        int index = 0;
        for (SpliceEdge edgeType : bleedEdges) {
            String edgeName = edgeType.name().toLowerCase(Locale.ROOT);
            BleedMarkAsset bleedMarkAsset = assets.bleedMarkAsset(edgeType);
            String offsetLabel = formatOffsetLabel(processConfig.bleedOffsetMm);
            marks.add(rectMark(processConfig.markPrefix + "-bleed-" + edgeName + "-start-" + offsetLabel + "mm-" + index + "-" + pieceMongoId,
                    bleedMarkAsset.img, bleedRectOnEdge(edgeType, width, height, bleedMarkAsset.width, bleedMarkAsset.height, true, processConfig.bleedOffsetMm, shapeEdgeSpans)));
            marks.add(rectMark(processConfig.markPrefix + "-bleed-" + edgeName + "-end-" + offsetLabel + "mm-" + index + "-" + pieceMongoId,
                    bleedMarkAsset.img, bleedRectOnEdge(edgeType, width, height, bleedMarkAsset.width, bleedMarkAsset.height, false, processConfig.bleedOffsetMm, shapeEdgeSpans)));
            index++;
        }
        for (SpliceEdge edgeType : coveredEdges) {
            EdgeAssets edgeAssets = assets.edgeAssets.get(edgeType);
            if (edgeAssets == null) {
                continue;
            }
            // 被出血边置换到右/下侧后，按视觉方向仍保持“线条、文字、文字”的顺序。
            marks.add(rectMark(processConfig.markPrefix + "-stripe-" + edgeType.name().toLowerCase(Locale.ROOT) + "-a-" + index + "-" + pieceMongoId,
                    edgeAssets.stripe, coveredRectOnEdge(edgeType, width, height, edgeAssets.stripeWidth, edgeAssets.stripeHeight, true, 19D, shapeEdgeSpans)));
            marks.add(rectMark(processConfig.markPrefix + "-stripe-" + edgeType.name().toLowerCase(Locale.ROOT) + "-b-" + index + "-" + pieceMongoId,
                    edgeAssets.stripe, coveredRectOnEdge(edgeType, width, height, edgeAssets.stripeWidth, edgeAssets.stripeHeight, false, 19D, shapeEdgeSpans)));
            marks.add(rectMark(processConfig.markPrefix + "-text-gray-" + edgeType.name().toLowerCase(Locale.ROOT) + "-a-" + index + "-" + pieceMongoId,
                    edgeAssets.grayText, coveredRectOnEdge(edgeType, width, height, edgeAssets.textWidth, edgeAssets.textHeight, true, 8D, shapeEdgeSpans)));
            marks.add(rectMark(processConfig.markPrefix + "-text-gray-" + edgeType.name().toLowerCase(Locale.ROOT) + "-b-" + index + "-" + pieceMongoId,
                    edgeAssets.grayText, coveredRectOnEdge(edgeType, width, height, edgeAssets.textWidth, edgeAssets.textHeight, false, 8D, shapeEdgeSpans)));
            marks.add(rectMark(processConfig.markPrefix + "-text-yellow-" + edgeType.name().toLowerCase(Locale.ROOT) + "-a-" + index + "-" + pieceMongoId,
                    edgeAssets.yellowText, coveredRectOnEdge(edgeType, width, height, edgeAssets.textWidth, edgeAssets.textHeight, true, 0D, shapeEdgeSpans)));
            marks.add(rectMark(processConfig.markPrefix + "-text-yellow-" + edgeType.name().toLowerCase(Locale.ROOT) + "-b-" + index + "-" + pieceMongoId,
                    edgeAssets.yellowText, coveredRectOnEdge(edgeType, width, height, edgeAssets.textWidth, edgeAssets.textHeight, false, 0D, shapeEdgeSpans)));
            index++;
        }
        return marks;
    }

    /**
     * 被出血边标识贴在被出血边与相邻边组成的两个内角处。
     *
     * <p>startCorner=true 表示贴当前边起点角，false 表示贴终点角；坐标沿边方向整体落在工件内部，
     * 不再把图片中心直接压在角点，避免一半图片跑到零件外侧。</p>
     */
    private Rect coveredRectOnEdge(SpliceEdge edgeType, double pieceWidth, double pieceHeight,
                                   double markWidth, double markHeight, boolean startCorner, double inwardOffset,
                                   ShapeEdgeSpans shapeEdgeSpans) {
        EdgeSpan span = shapeEdgeSpans == null ? null : shapeEdgeSpans.span(edgeType);
        switch (edgeType) {
            case RIGHT:
                return new Rect(pieceWidth - inwardOffset - markWidth, edgeStartY(span, pieceHeight, markHeight, startCorner), markWidth, markHeight);
            case BOTTOM:
                return new Rect(edgeStartX(span, pieceWidth, markWidth, startCorner), pieceHeight - inwardOffset - markHeight, markWidth, markHeight);
            case LEFT:
                return new Rect(inwardOffset, edgeStartY(span, pieceHeight, markHeight, startCorner), markWidth, markHeight);
            case TOP:
            default:
                return new Rect(edgeStartX(span, pieceWidth, markWidth, startCorner), inwardOffset, markWidth, markHeight);
        }
    }

    /**
     * 出血边在自身的两个相邻角各放一条工艺配置距离的 1x6 黑白条。
     *
     * <p>{@code edgeOffset} 表示从出血边向画面内侧量起的距离，{@code fromStartCorner}
     * 用来选择贴住出血边的起点相邻边还是终点相邻边。因此每条出血边会生成 2 个 mark，
     * 分别位于起点角和终点角。</p>
     */
    private Rect bleedRectOnEdge(SpliceEdge edgeType, double pieceWidth, double pieceHeight,
                                 double markWidth, double markHeight, boolean fromStartCorner, double edgeOffset,
                                 ShapeEdgeSpans shapeEdgeSpans) {
        EdgeSpan span = shapeEdgeSpans == null ? null : shapeEdgeSpans.span(edgeType);
        switch (edgeType) {
            case RIGHT:
                return new Rect(pieceWidth - edgeOffset - markWidth,
                        edgeStartY(span, pieceHeight, markHeight, fromStartCorner), markWidth, markHeight);
            case BOTTOM:
                return new Rect(edgeStartX(span, pieceWidth, markWidth, fromStartCorner),
                        pieceHeight - edgeOffset - markHeight, markWidth, markHeight);
            case LEFT:
                return new Rect(edgeOffset,
                        edgeStartY(span, pieceHeight, markHeight, fromStartCorner), markWidth, markHeight);
            case TOP:
            default:
                return new Rect(edgeStartX(span, pieceWidth, markWidth, fromStartCorner),
                        edgeOffset, markWidth, markHeight);
        }
    }

    private Map<String, Object> rectMark(String id, String img, Rect rect) {
        return com.mes.application.command.graphics.RemoteGraphics.params("id", id, "assetKey", img,
                "x", rect.x, "y", rect.y, "width", rect.width, "height", rect.height);
    }

    private double edgeStartY(EdgeSpan span, double pieceHeight, double markHeight, boolean startCorner) {
        if (span == null || !span.valid()) {
            return startCorner ? 0D : pieceHeight - markHeight;
        }
        return startCorner ? span.start : Math.max(span.start, span.end - markHeight);
    }

    private double edgeStartX(EdgeSpan span, double pieceWidth, double markWidth, boolean startCorner) {
        if (span == null || !span.valid()) {
            return startCorner ? 0D : pieceWidth - markWidth;
        }
        return startCorner ? span.start : Math.max(span.start, span.end - markWidth);
    }





    /**
     * 拼接自身标识的出血/被出血边判断。
     *
     * <p>这里只影响拼接标识放置，不改变留白/打扣策略对 blood 的既有解析逻辑。
     * 切割方向只以同组 seq=1 工件的 blood 为准：x!=0,y=0 表示拼接缝在左右方向，按左右边处理；x=0,y!=0 表示拼接缝在上下方向，按上下边处理。
     * 由于调用抠图接口时拼接 blood 统一改为负数，竖切时算法侧出血边变为左边、被出血边变为右边；
     * 横切时算法侧出血边变为上边、被出血边变为下边；
     * 因此首片只有被出血边，尾片只有出血边，中间片同时具备两类边。</p>
     */
    private SpliceEdges resolveSpliceEdges(ProcedureFlow procedureFlow, SpliceProcessConfig processConfig, ProductionPiece piece, Blood firstSeqBlood) {
        Set<SpliceEdge> bleedEdges = EnumSet.noneOf(SpliceEdge.class);
        Set<SpliceEdge> coveredEdges = EnumSet.noneOf(SpliceEdge.class);
        Integer currentSeq = piece.getSeq();
        Integer maxSeq = extractMaxSeqInGroup(piece.getGroup());
        if (currentSeq == null || maxSeq == null || maxSeq <= 0) {
            return new SpliceEdges(bleedEdges, coveredEdges);
        }

        SpliceCutDirection cutDirection = resolveCutDirection(firstSeqBlood);
        if (cutDirection == SpliceCutDirection.UNKNOWN) {
            cutDirection = resolveCutDirection(procedureFlow, processConfig);
        }
        if (cutDirection == SpliceCutDirection.UNKNOWN) {
            return new SpliceEdges(bleedEdges, coveredEdges);
        }
        for (SpliceEdge bleedEdge : resolveBleedEdges(cutDirection, firstSeqBlood)) {
            SpliceEdge coveredEdge = oppositeEdge(bleedEdge);
            if (currentSeq == 1) {
                coveredEdges.add(coveredEdge);
            } else if (currentSeq.intValue() == maxSeq.intValue()) {
                if (processConfig.bleedMarksEnabled) {
                    bleedEdges.add(bleedEdge);
                }
            } else if (currentSeq > 1 && currentSeq < maxSeq) {
                coveredEdges.add(coveredEdge);
                if (processConfig.bleedMarksEnabled) {
                    bleedEdges.add(bleedEdge);
                }
            }
        }
        return new SpliceEdges(bleedEdges, coveredEdges);
    }

    private SpliceCutDirection resolveCutDirection(Blood firstSeqBlood) {
        if (firstSeqBlood == null) {
            return SpliceCutDirection.UNKNOWN;
        }
        Integer x = firstSeqBlood.getX();
        Integer y = firstSeqBlood.getY();
        if (isNonZero(x) && isZero(y)) {
            return SpliceCutDirection.VERTICAL;
        }
        if (isZero(x) && isNonZero(y)) {
            return SpliceCutDirection.HORIZONTAL;
        }
        return SpliceCutDirection.UNKNOWN;
    }

    private SpliceCutDirection resolveCutDirection(ProcedureFlow procedureFlow, SpliceProcessConfig processConfig) {
        if (procedureFlow == null || procedureFlow.getNodes() == null || processConfig == null) {
            return SpliceCutDirection.UNKNOWN;
        }
        boolean hasXs = false;
        boolean hasYs = false;
        for (ProcedureFlowNode node : procedureFlow.getNodes()) {
            if (node == null || !processConfig.nodeName.equals(node.getNodeName()) || node.getParamConfigs() == null) {
                continue;
            }
            for (com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO.ProcessParamConfigDTO config : node.getParamConfigs()) {
                Object param = config == null ? null : config.getParam();
                if (param instanceof Map) {
                    Object xs = ((Map<?, ?>) param).get("xs");
                    Object ys = ((Map<?, ?>) param).get("ys");
                    hasXs = hasXs || (xs instanceof Iterable && ((Iterable<?>) xs).iterator().hasNext());
                    hasYs = hasYs || (ys instanceof Iterable && ((Iterable<?>) ys).iterator().hasNext());
                }
            }
        }
        if (hasXs && hasYs) {
            return SpliceCutDirection.BOTH;
        }
        if (hasXs) {
            return SpliceCutDirection.VERTICAL;
        }
        if (hasYs) {
            return SpliceCutDirection.HORIZONTAL;
        }
        return SpliceCutDirection.UNKNOWN;
    }

    private Set<SpliceEdge> resolveBleedEdges(SpliceCutDirection cutDirection, Blood firstSeqBlood) {
        Set<SpliceEdge> edges = EnumSet.noneOf(SpliceEdge.class);
        if (cutDirection == SpliceCutDirection.HORIZONTAL || cutDirection == SpliceCutDirection.BOTH) {
            edges.add(resolveHorizontalBleedEdge(firstSeqBlood));
        }
        if (cutDirection == SpliceCutDirection.VERTICAL || cutDirection == SpliceCutDirection.BOTH) {
            edges.add(resolveVerticalBleedEdge(firstSeqBlood));
        }
        return edges;
    }

    private SpliceEdge resolveVerticalBleedEdge(Blood firstSeqBlood) {
        Integer x = firstSeqBlood == null ? null : firstSeqBlood.getX();
        if (x != null && x > 0) {
            return SpliceEdge.RIGHT;
        }
        return SpliceEdge.LEFT;
    }

    private SpliceEdge resolveHorizontalBleedEdge(Blood firstSeqBlood) {
        Integer y = firstSeqBlood == null ? null : firstSeqBlood.getY();
        if (y != null && y > 0) {
            return SpliceEdge.BOTTOM;
        }
        return SpliceEdge.TOP;
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

    private String resolveSvg(String url) {
        try {
            byte[] bytes = com.mes.application.command.graphics.SvgResourceReader.read(url);
            return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("下载拼接 mask SVG 失败: {}, error={}", url, e.getMessage());
            return null;
        }
    }

    private void updateMarks(ProductionPiece piece, MarkAssets assets) {
        Map<String, String> marks = piece.getMarks();
        if (marks == null) {
            marks = new LinkedHashMap<>();
            piece.setMarks(marks);
        }
        marks.put("superWidthSpliceHorizontalDarkMark", assets.horizontalDarkMark);
        marks.put("superWidthSpliceVerticalDarkMark", assets.verticalDarkMark);
        for (Map.Entry<SpliceEdge, EdgeAssets> entry : assets.edgeAssets.entrySet()) {
            EdgeAssets edgeAssets = entry.getValue();
            String suffix = entry.getKey().name().toLowerCase(Locale.ROOT);
            marks.put("superWidthSpliceStripe-" + suffix, edgeAssets.stripe);
            marks.put("superWidthSpliceYellowText-" + suffix, edgeAssets.yellowText);
            marks.put("superWidthSpliceGrayText-" + suffix, edgeAssets.grayText);
        }
    }

    private ShapeEdgeSpans resolveShapeEdgeSpans(String svg, double pieceWidth, double pieceHeight) {
        ShapeEdgeSpans spans = new ShapeEdgeSpans();
        if (StringUtils.isBlank(svg)) {
            return spans;
        }
        List<Segment> segments = new ArrayList<>();
        Matcher pathMatcher = SVG_PATH_PATTERN.matcher(svg);
        while (pathMatcher.find()) {
            String d = attributeValue(pathMatcher.group(1), "d");
            segments.addAll(parsePathSegments(d));
        }
        if (segments.isEmpty()) {
            return spans;
        }
        double tolerance = Math.max(0.5D, Math.max(pieceWidth, pieceHeight) * 0.002D);
        for (Segment segment : segments) {
            if (segment.isVertical()) {
                if (near(segment.x1, 0D, tolerance)) {
                    spans.include(SpliceEdge.LEFT, Math.min(segment.y1, segment.y2), Math.max(segment.y1, segment.y2));
                }
                if (near(segment.x1, pieceWidth, tolerance)) {
                    spans.include(SpliceEdge.RIGHT, Math.min(segment.y1, segment.y2), Math.max(segment.y1, segment.y2));
                }
            } else if (segment.isHorizontal()) {
                if (near(segment.y1, 0D, tolerance)) {
                    spans.include(SpliceEdge.TOP, Math.min(segment.x1, segment.x2), Math.max(segment.x1, segment.x2));
                }
                if (near(segment.y1, pieceHeight, tolerance)) {
                    spans.include(SpliceEdge.BOTTOM, Math.min(segment.x1, segment.x2), Math.max(segment.x1, segment.x2));
                }
            }
        }
        return spans;
    }

    private List<Segment> parsePathSegments(String d) {
        List<Segment> segments = new ArrayList<>();
        if (StringUtils.isBlank(d)) {
            return segments;
        }
        List<String> tokens = new ArrayList<>();
        Matcher matcher = SVG_PATH_TOKEN_PATTERN.matcher(d);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        double currentX = 0D;
        double currentY = 0D;
        double startX = 0D;
        double startY = 0D;
        char command = 0;
        int i = 0;
        while (i < tokens.size()) {
            String token = tokens.get(i);
            if (isPathCommand(token)) {
                command = token.charAt(0);
                i++;
                if (command == 'Z' || command == 'z') {
                    segments.add(new Segment(currentX, currentY, startX, startY));
                    currentX = startX;
                    currentY = startY;
                }
                continue;
            }
            switch (command) {
                case 'M':
                case 'm': {
                    if (i + 1 >= tokens.size()) {
                        return segments;
                    }
                    double x = parsePathNumber(tokens.get(i++));
                    double y = parsePathNumber(tokens.get(i++));
                    if (command == 'm') {
                        x += currentX;
                        y += currentY;
                    }
                    currentX = x;
                    currentY = y;
                    startX = x;
                    startY = y;
                    command = command == 'm' ? 'l' : 'L';
                    break;
                }
                case 'L':
                case 'l': {
                    if (i + 1 >= tokens.size()) {
                        return segments;
                    }
                    double x = parsePathNumber(tokens.get(i++));
                    double y = parsePathNumber(tokens.get(i++));
                    if (command == 'l') {
                        x += currentX;
                        y += currentY;
                    }
                    segments.add(new Segment(currentX, currentY, x, y));
                    currentX = x;
                    currentY = y;
                    break;
                }
                case 'H':
                case 'h': {
                    double x = parsePathNumber(tokens.get(i++));
                    if (command == 'h') {
                        x += currentX;
                    }
                    segments.add(new Segment(currentX, currentY, x, currentY));
                    currentX = x;
                    break;
                }
                case 'V':
                case 'v': {
                    double y = parsePathNumber(tokens.get(i++));
                    if (command == 'v') {
                        y += currentY;
                    }
                    segments.add(new Segment(currentX, currentY, currentX, y));
                    currentY = y;
                    break;
                }
                default:
                    i++;
                    break;
            }
        }
        return segments;
    }

    private boolean isPathCommand(String token) {
        return token != null && token.length() == 1 && Character.isLetter(token.charAt(0));
    }

    private double parsePathNumber(String token) {
        try {
            return Double.parseDouble(token);
        } catch (Exception e) {
            return 0D;
        }
    }

    private boolean near(double value, double target, double tolerance) {
        return Math.abs(value - target) <= tolerance;
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

    private String formatOffsetLabel(double value) {
        return format(value).replace('.', '_');
    }

    private boolean isZero(Integer value) {
        return value != null && value == 0;
    }

    private boolean isNonZero(Integer value) {
        return value != null && value != 0;
    }

    private static class SpliceProcessConfig {
        private final String nodeName;
        private final String markPrefix;
        private final double bleedOffsetMm;
        private final boolean bleedMarksEnabled;

        private SpliceProcessConfig(String nodeName, String markPrefix, double bleedOffsetMm, boolean bleedMarksEnabled) {
            this.nodeName = nodeName;
            this.markPrefix = markPrefix;
            this.bleedOffsetMm = bleedOffsetMm;
            this.bleedMarksEnabled = bleedMarksEnabled;
        }
    }

    private enum SpliceEdge {
        TOP, RIGHT, BOTTOM, LEFT
    }

    private enum SpliceCutDirection {
        VERTICAL, HORIZONTAL, BOTH, UNKNOWN
    }

    private static class SpliceEdges {
        private final Set<SpliceEdge> bleedEdges;
        private final Set<SpliceEdge> coveredEdges;

        private SpliceEdges(Set<SpliceEdge> bleedEdges, Set<SpliceEdge> coveredEdges) {
            this.bleedEdges = bleedEdges;
            this.coveredEdges = coveredEdges;
        }
    }

    private static class MarkAssets {
        private final String horizontalDarkMark;
        private final String verticalDarkMark;
        private final Map<SpliceEdge, EdgeAssets> edgeAssets;
        private final double horizontalDarkWidth;
        private final double horizontalDarkHeight;
        private final double verticalDarkWidth;
        private final double verticalDarkHeight;

        private MarkAssets(String horizontalDarkMark, String verticalDarkMark, Map<SpliceEdge, EdgeAssets> edgeAssets,
                           double horizontalDarkWidth, double horizontalDarkHeight, double verticalDarkWidth, double verticalDarkHeight) {
            this.horizontalDarkMark = horizontalDarkMark;
            this.verticalDarkMark = verticalDarkMark;
            this.edgeAssets = edgeAssets;
            this.horizontalDarkWidth = horizontalDarkWidth;
            this.horizontalDarkHeight = horizontalDarkHeight;
            this.verticalDarkWidth = verticalDarkWidth;
            this.verticalDarkHeight = verticalDarkHeight;
        }

        private BleedMarkAsset bleedMarkAsset(SpliceEdge edgeType) {
            if (edgeType == SpliceEdge.LEFT || edgeType == SpliceEdge.RIGHT) {
                return new BleedMarkAsset(verticalDarkMark, verticalDarkWidth, verticalDarkHeight);
            }
            return new BleedMarkAsset(horizontalDarkMark, horizontalDarkWidth, horizontalDarkHeight);
        }
    }

    private static class BleedMarkAsset {
        private final String img;
        private final double width;
        private final double height;

        private BleedMarkAsset(String img, double width, double height) {
            this.img = img;
            this.width = width;
            this.height = height;
        }
    }

    private static class EdgeAssets {
        private final String stripe;
        private final String yellowText;
        private final String grayText;
        private final double stripeWidth;
        private final double stripeHeight;
        private final double textWidth;
        private final double textHeight;

        private EdgeAssets(String stripe, String yellowText, String grayText,
                           double stripeWidth, double stripeHeight, double textWidth, double textHeight) {
            this.stripe = stripe;
            this.yellowText = yellowText;
            this.grayText = grayText;
            this.stripeWidth = stripeWidth;
            this.stripeHeight = stripeHeight;
            this.textWidth = textWidth;
            this.textHeight = textHeight;
        }
    }

    private static class ShapeEdgeSpans {
        private final Map<SpliceEdge, EdgeSpan> spans = new EnumMap<>(SpliceEdge.class);

        private void include(SpliceEdge edge, double start, double end) {
            if (Math.abs(end - start) < 0.000001D) {
                return;
            }
            EdgeSpan span = spans.get(edge);
            if (span == null) {
                spans.put(edge, new EdgeSpan(start, end));
            } else {
                span.include(start, end);
            }
        }

        private EdgeSpan span(SpliceEdge edge) {
            return spans.get(edge);
        }
    }

    private static class EdgeSpan {
        private double start;
        private double end;

        private EdgeSpan(double start, double end) {
            this.start = Math.min(start, end);
            this.end = Math.max(start, end);
        }

        private void include(double start, double end) {
            this.start = Math.min(this.start, Math.min(start, end));
            this.end = Math.max(this.end, Math.max(start, end));
        }

        private boolean valid() {
            return end > start;
        }
    }

    private static class Segment {
        private final double x1;
        private final double y1;
        private final double x2;
        private final double y2;

        private Segment(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        private boolean isVertical() {
            return Math.abs(x1 - x2) < 0.000001D;
        }

        private boolean isHorizontal() {
            return Math.abs(y1 - y2) < 0.000001D;
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

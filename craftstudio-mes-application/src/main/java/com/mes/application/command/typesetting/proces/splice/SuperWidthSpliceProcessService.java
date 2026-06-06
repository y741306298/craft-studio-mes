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
    private static final double TEXT_PNG_DPI = 300D;
    private static final double MM_PER_INCH = 25.4D;
    private static final double BLEED_EDGE_INSET_MM = 1D;

    private final RestTemplate restTemplate;
    private final OssTagUploadService ossTagUploadService;

    public void process(OrderItem orderItem, ProcedureFlow procedureFlow, ProductionPiece piece, Blood firstSeqBlood) {
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
        SpliceEdges spliceEdges = resolveSpliceEdges(piece, firstSeqBlood);
        if (spliceEdges.bleedEdges.isEmpty() && spliceEdges.coveredEdges.isEmpty()) {
            return;
        }

        String marksSvg = buildMarksSvg(pieceMongoId, width, height, piece.getGroup(), spliceEdges.bleedEdges, spliceEdges.coveredEdges, assets);
        String originalContentImg = resolveOriginalContentImg(piece, originalMaskUrl);
        String markedSvg = appendMarksSvg(originalSvg, pieceMongoId, originalContentImg, marksSvg);
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
        double textWidthMm = Math.max(24D, groupText.length() * 8D);
        double textHeightMm = 10D;
        Map<SpliceEdge, EdgeAssets> edgeAssets = new EnumMap<>(SpliceEdge.class);
        for (SpliceEdge edge : SpliceEdge.values()) {
            boolean verticalEdge = edge == SpliceEdge.LEFT || edge == SpliceEdge.RIGHT;
            double angle = verticalEdge ? 90D : 0D;
            BufferedImage yellowTextImage = rotateImageByAngle(createTextImage(textWidthMm, textHeightMm, groupText, createYellowColor(20)), angle);
            BufferedImage grayTextImage = rotateImageByAngle(createTextImage(textWidthMm, textHeightMm, groupText, createGrayColor(20)), angle);
            BufferedImage stripeImage = rotateImageByAngle(createStripeImage(6, 1), angle);
            String yellowText = ossTagUploadService.uploadTagPng(businessId, toPng(yellowTextImage), markSubDir);
            String grayText = ossTagUploadService.uploadTagPng(businessId, toPng(grayTextImage), markSubDir);
            String stripe = ossTagUploadService.uploadTagPng(businessId, toPng(stripeImage), markSubDir);
            double displayTextWidthMm = verticalEdge ? textHeightMm : textWidthMm;
            double displayTextHeightMm = verticalEdge ? textWidthMm : textHeightMm;
            edgeAssets.put(edge, new EdgeAssets(stripe, yellowText, grayText, stripeImage.getWidth(), stripeImage.getHeight(),
                    displayTextWidthMm, displayTextHeightMm));
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
            builder.append(buildRectMarkGroup("super-width-splice-bleed-" + edgeType.name().toLowerCase(Locale.ROOT) + "-a-" + index + "-" + pieceMongoId,
                    assets.darkMark, bleedRectOnEdge(edgeType, width, height, assets.darkWidth, assets.darkHeight, true, 20D)));
            builder.append(buildRectMarkGroup("super-width-splice-bleed-" + edgeType.name().toLowerCase(Locale.ROOT) + "-b-" + index + "-" + pieceMongoId,
                    assets.darkMark, bleedRectOnEdge(edgeType, width, height, assets.darkWidth, assets.darkHeight, false, 30D)));
            index++;
        }
        for (SpliceEdge edgeType : coveredEdges) {
            EdgeAssets edgeAssets = assets.edgeAssets.get(edgeType);
            if (edgeAssets == null) {
                continue;
            }
            builder.append(buildRectMarkGroup("super-width-splice-text-yellow-" + edgeType.name().toLowerCase(Locale.ROOT) + "-a-" + index + "-" + pieceMongoId,
                    edgeAssets.yellowText, coveredRectOnEdge(edgeType, width, height, edgeAssets.textWidth, edgeAssets.textHeight, true, 0D)));
            builder.append(buildRectMarkGroup("super-width-splice-text-yellow-" + edgeType.name().toLowerCase(Locale.ROOT) + "-b-" + index + "-" + pieceMongoId,
                    edgeAssets.yellowText, coveredRectOnEdge(edgeType, width, height, edgeAssets.textWidth, edgeAssets.textHeight, false, 0D)));
            builder.append(buildRectMarkGroup("super-width-splice-text-gray-" + edgeType.name().toLowerCase(Locale.ROOT) + "-a-" + index + "-" + pieceMongoId,
                    edgeAssets.grayText, coveredRectOnEdge(edgeType, width, height, edgeAssets.textWidth, edgeAssets.textHeight, true, 10D)));
            builder.append(buildRectMarkGroup("super-width-splice-text-gray-" + edgeType.name().toLowerCase(Locale.ROOT) + "-b-" + index + "-" + pieceMongoId,
                    edgeAssets.grayText, coveredRectOnEdge(edgeType, width, height, edgeAssets.textWidth, edgeAssets.textHeight, false, 10D)));
            builder.append(buildRectMarkGroup("super-width-splice-stripe-" + edgeType.name().toLowerCase(Locale.ROOT) + "-a-" + index + "-" + pieceMongoId,
                    edgeAssets.stripe, coveredRectOnEdge(edgeType, width, height, edgeAssets.stripeWidth, edgeAssets.stripeHeight, true, 20D)));
            builder.append(buildRectMarkGroup("super-width-splice-stripe-" + edgeType.name().toLowerCase(Locale.ROOT) + "-b-" + index + "-" + pieceMongoId,
                    edgeAssets.stripe, coveredRectOnEdge(edgeType, width, height, edgeAssets.stripeWidth, edgeAssets.stripeHeight, false, 20D)));
            index++;
        }
        return builder.toString();
    }

    /**
     * 被出血边标识贴在被出血边与相邻边组成的两个内角处。
     *
     * <p>startCorner=true 表示贴当前边起点角，false 表示贴终点角；坐标沿边方向整体落在工件内部，
     * 不再把图片中心直接压在角点，避免一半图片跑到零件外侧。</p>
     */
    private Rect coveredRectOnEdge(SpliceEdge edgeType, double pieceWidth, double pieceHeight,
                                   double markWidth, double markHeight, boolean startCorner, double inwardOffset) {
        switch (edgeType) {
            case RIGHT:
                return new Rect(pieceWidth - inwardOffset - markWidth, startCorner ? 0D : pieceHeight - markHeight, markWidth, markHeight);
            case BOTTOM:
                return new Rect(startCorner ? 0D : pieceWidth - markWidth, pieceHeight - inwardOffset - markHeight, markWidth, markHeight);
            case LEFT:
                return new Rect(inwardOffset, startCorner ? 0D : pieceHeight - markHeight, markWidth, markHeight);
            case TOP:
            default:
                return new Rect(startCorner ? 0D : pieceWidth - markWidth, inwardOffset, markWidth, markHeight);
        }
    }

    /**
     * 出血边在沿出血边方向、距离相邻角 20mm / 30mm 的位置放 1x6 黑白条。
     *
     * <p>{@code edgeOffset} 表示沿出血边方向从相邻角量起的距离，不是从出血边向画面内缩的距离：
     * RIGHT/LEFT 边调整 Y 坐标，TOP/BOTTOM 边调整 X 坐标。垂直于出血边的方向只内缩 1mm，
     * 用来避免贴边路径被裁切到画面外。</p>
     */
    private Rect bleedRectOnEdge(SpliceEdge edgeType, double pieceWidth, double pieceHeight,
                                 double markWidth, double markHeight, boolean fromStartCorner, double edgeOffset) {
        switch (edgeType) {
            case RIGHT:
                return new Rect(pieceWidth - markWidth - BLEED_EDGE_INSET_MM,
                        fromStartCorner ? edgeOffset : pieceHeight - edgeOffset - markHeight, markWidth, markHeight);
            case BOTTOM:
                return new Rect(fromStartCorner ? edgeOffset : pieceWidth - edgeOffset - markWidth,
                        pieceHeight - markHeight - BLEED_EDGE_INSET_MM, markWidth, markHeight);
            case LEFT:
                return new Rect(BLEED_EDGE_INSET_MM,
                        fromStartCorner ? edgeOffset : pieceHeight - edgeOffset - markHeight, markWidth, markHeight);
            case TOP:
            default:
                return new Rect(fromStartCorner ? edgeOffset : pieceWidth - edgeOffset - markWidth,
                        BLEED_EDGE_INSET_MM, markWidth, markHeight);
        }
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

    /**
     * 超幅拼接自身标识的出血/被出血边判断。
     *
     * <p>这里只影响超幅拼接标识放置，不改变留白/打扣策略对 blood 的既有解析逻辑。
     * 切割方向只以同组 seq=1 工件的 blood 为准：x!=0,y=0 表示拼接缝在左右方向，按左右边处理；x=0,y!=0 表示拼接缝在上下方向，按上下边处理。</p>
     */
    private SpliceEdges resolveSpliceEdges(ProductionPiece piece, Blood firstSeqBlood) {
        Set<SpliceEdge> bleedEdges = EnumSet.noneOf(SpliceEdge.class);
        Set<SpliceEdge> coveredEdges = EnumSet.noneOf(SpliceEdge.class);
        Integer currentSeq = piece.getSeq();
        Integer maxSeq = extractMaxSeqInGroup(piece.getGroup());
        if (currentSeq == null || maxSeq == null || maxSeq <= 0) {
            return new SpliceEdges(bleedEdges, coveredEdges);
        }

        SpliceCutDirection cutDirection = resolveCutDirection(firstSeqBlood);
        if (cutDirection == SpliceCutDirection.UNKNOWN) {
            return new SpliceEdges(bleedEdges, coveredEdges);
        }
        SpliceEdge firstBleedEdge = firstBleedEdge(cutDirection);
        SpliceEdge lastCoveredEdge = oppositeEdge(firstBleedEdge);
        if (currentSeq == 1) {
            bleedEdges.add(firstBleedEdge);
        } else if (currentSeq.intValue() == maxSeq.intValue()) {
            coveredEdges.add(lastCoveredEdge);
        } else if (currentSeq > 1 && currentSeq < maxSeq) {
            coveredEdges.add(lastCoveredEdge);
            bleedEdges.add(firstBleedEdge);
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

    private SpliceEdge firstBleedEdge(SpliceCutDirection cutDirection) {
        return cutDirection == SpliceCutDirection.HORIZONTAL ? SpliceEdge.BOTTOM : SpliceEdge.RIGHT;
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

    private BufferedImage createTextImage(double widthMm, double heightMm, String text, Color textColor) {
        int width = convertTextMmToPixels(widthMm);
        int height = convertTextMmToPixels(heightMm);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, width, height);
        g.setComposite(AlphaComposite.SrcOver);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, height - convertTextMmToPixels(1D))));
        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth(text);
        int textH = fm.getAscent();
        int x = Math.max(0, (width - textW) / 2);
        int y = Math.max(textH, Math.min(height - convertTextMmToPixels(0.5D), (height + textH) / 2 - 1));
        g.setColor(textColor);
        g.drawString(text, x, y);
        g.dispose();
        return image;
    }

    private int convertTextMmToPixels(double valueMm) {
        return Math.max(1, (int) Math.ceil(valueMm / MM_PER_INCH * TEXT_PNG_DPI));
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
            marks.put("superWidthSpliceYellowText-" + suffix, edgeAssets.yellowText);
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

    private Color createYellowColor(int yellowPercent) {
        int yellow = Math.max(0, Math.min(100, yellowPercent));
        int blue = (int) Math.round(255 * (1 - yellow / 100.0));
        return new Color(255, 255, blue);
    }

    private Color createGrayColor(int blackPercent) {
        int black = Math.max(0, Math.min(100, blackPercent));
        int v = (int) Math.round(255 * (1 - black / 100.0));
        return new Color(v, v, v);
    }

    private enum SpliceEdge {
        TOP, RIGHT, BOTTOM, LEFT
    }

    private enum SpliceCutDirection {
        VERTICAL, HORIZONTAL, UNKNOWN
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

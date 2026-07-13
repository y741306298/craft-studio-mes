package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.shared.utils.IdGenerator;
import com.mes.domain.manufacturer.productionPiece.entity.Blood;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.piliofpala.craftstudio.shared.domain.file.vo.FilePreview;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import io.micrometer.common.util.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 固定尺寸留白实体策略基类。
 *
 * <p>设计目的：</p>
 * <ul>
 *     <li>“留白2cm”“留白5cm”“留白10cm”“留白15cm”等固定尺寸留白的处理流程完全一致，仅匹配关键字和外扩毫米数不同。</li>
 *     <li>该基类集中实现 SVG 拉取、尺寸解析、非出血边外扩、SVG 生成、mark PNG 生成、OSS 上传与工件字段回写。</li>
 *     <li>具体规格策略只需要提供规格名称、外扩毫米数和匹配关键字，避免多套留白策略复制同一份 SVG 处理逻辑。</li>
 * </ul>
 */
public abstract class AbstractFixedLiubaiProcessStrategy extends AbstractLiubaiProcessStrategy {
    private static final float DEFAULT_BORDER_STROKE_WIDTH_PX = 1F;
    private static final double DEFAULT_BORDER_STROKE_WIDTH_SVG = 1.23D;
    /**
     * PNG 标记图使用的输出 DPI。
     *
     * <p>外扩 SVG 的宽高单位按业务约定为 mm，生成 PNG 时需要按照 36dpi 将 mm 换算成像素。</p>
     */
    private static final double MARK_PNG_DPI = 36D;

    /**
     * 留白后附加工艺文字标签 PNG 使用的输出 DPI。
     */
    private static final double LIUBAI_TAG_PNG_DPI = 300D;

    /**
     * 留白后附加工艺标签贴边默认边长，单位 mm。
     */
    private static final double DEFAULT_LIUBAI_TAG_EDGE_SIZE_MM = 10D;

    /**
     * 留白标签距离相邻边的安全距离，单位 mm。
     */
    private static final double LIUBAI_TAG_ADJACENT_EDGE_GAP_MM = 10D;

    /**
     * 毫米与英寸换算常量。
     */
    private static final double MM_PER_INCH = 25.4D;

    /**
     * 从 SVG width 属性中解析数值，兼容纯数字、px、mm 三种写法。
     */
    private static final Pattern SVG_WIDTH_PATTERN = Pattern.compile("width\\s*=\\s*[\"']\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:px|mm)?\\s*[\"']", Pattern.CASE_INSENSITIVE);

    /**
     * 从 SVG height 属性中解析数值，兼容纯数字、px、mm 三种写法。
     */
    private static final Pattern SVG_HEIGHT_PATTERN = Pattern.compile("height\\s*=\\s*[\"']\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:px|mm)?\\s*[\"']", Pattern.CASE_INSENSITIVE);

    /**
     * 匹配原 SVG 根节点开始标签，用于只更新根节点尺寸属性并保留原有图形分组。
     */
    private static final Pattern SVG_OPEN_PATTERN = Pattern.compile("<svg\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    /**
     * 匹配原 SVG 根节点关闭标签，用于定位根节点内容范围。
     */
    private static final Pattern SVG_CLOSE_PATTERN = Pattern.compile("</svg\\s*>", Pattern.CASE_INSENSITIVE);

    /**
     * 判断原始 SVG 中是否已经存在分组节点。
     */
    private static final Pattern SVG_GROUP_PATTERN = Pattern.compile("<g\\b", Pattern.CASE_INSENSITIVE);

    /**
     * 匹配可直接转换为 path 的 rect 节点。
     */
    private static final Pattern SVG_RECT_PATTERN = Pattern.compile("<rect\\b([^>]*)\\s*/>|<rect\\b([^>]*)>\\s*</rect\\s*>", Pattern.CASE_INSENSITIVE);

    /**
     * 匹配 SVG/XML 节点属性。
     */
    private static final Pattern SVG_ATTRIBUTE_PATTERN = Pattern.compile("\\s+([A-Za-z_:][-A-Za-z0-9_:.]*)\\s*=\\s*([\"']).*?\\2", Pattern.CASE_INSENSITIVE);

    /**
     * 从超幅拼接分组号中解析当前组的最大分片序号，例如 12#1-3 中的 3。
     */
    private static final Pattern MAX_SEQ_PATTERN = Pattern.compile("#\\s*\\d+-(\\d+)");

    /**
     * 用于当 maskImageFile.rawFile 是远程 URL 时拉取原 SVG 内容。
     */
    private final RestTemplate restTemplate;

    /**
     * 用于上传新生成的留白 mask SVG。
     */
    private final OssTagUploadService ossTagUploadService;

    /**
     * 构造固定尺寸留白策略基类。
     *
     * @param restTemplate 远程 SVG 拉取客户端
     * @param ossTagUploadService SVG 上传服务
     */
    protected AbstractFixedLiubaiProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        this.restTemplate = restTemplate;
        this.ossTagUploadService = ossTagUploadService;
    }

    /**
     * 判断工艺流程是否命中当前固定尺寸留白规格。
     *
     * <p>具体策略通过 {@link #matchKeywords()} 提供规格关键字，例如 2cm / 20mm 或 5cm / 50mm。</p>
     *
     * @param procedureFlow 已解析工艺流程
     * @return {@code true} 表示命中当前固定尺寸留白实体策略
     */
    @Override
    protected boolean matchesLiubaiValue(ProcedureFlow procedureFlow) {
        for (String keyword : matchKeywords()) {
            if (containsInNodeOrParams(procedureFlow, keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 执行固定尺寸留白外扩处理。
     *
     * <p>处理步骤：</p>
     * <ol>
     *     <li>读取当前 productionPiece.maskImageFile.rawFile 指向的原始 mask SVG。</li>
     *     <li>解析原始 SVG 宽高，解析失败时退回使用 productionPiece.width/height。</li>
     *     <li>根据是否需要跳过出血边计算四边外扩量。</li>
     *     <li>生成与外扩矩形同宽高的 mark PNG，上传后保存到 productionPiece.marks。</li>
     *     <li>更新 SVG 根节点尺寸/viewBox；原 SVG 已有 g 时将留白 g 插入为根节点首个子 g，无 g 时重构为分组结构。</li>
     *     <li>上传新 SVG，回写 productionPiece.maskImageFile，并更新生产工件宽高。</li>
     * </ol>
     *
     * @param context 留白处理上下文
     */
    @Override
    public void process(LiubaiProcessContext context) {
        ProductionPiece piece = context.getProductionPiece();
        if (piece == null) {
            return;
        }
        String originalMaskUrl = resolveOriginalMaskRef(context, piece);
        if (StringUtils.isBlank(originalMaskUrl)) {
            return;
        }
        String originalSvg = resolveSvg(originalMaskUrl);
        if (StringUtils.isBlank(originalSvg)) {
            return;
        }
        double originalWidth = resolveOriginalWidth(context, originalSvg, piece);
        double originalHeight = resolveOriginalHeight(context, originalSvg, piece);
        if (originalWidth <= 0 || originalHeight <= 0) {
            return;
        }

        ExpandMargins margins = resolveMargins(piece, context);
        String pieceMongoId = ensureProductionPieceMongoId(piece);
        String productionPieceId = ensureProductionPieceBusinessId(piece);
        String manufacturerMetaId = StringUtils.isBlank(piece.getManufacturerId()) ? "default" : piece.getManufacturerId();
        String markPngUrl = uploadOuterRectMarkPng(productionPieceId, manufacturerMetaId, originalWidth, originalHeight, margins);
        updateMarks(piece, markPngUrl);
        double expandedWidth = originalWidth + margins.left + margins.right;
        double expandedHeight = originalHeight + margins.top + margins.bottom;
        LiubaiTagAssets tagAssets = shouldUploadLiubaiTagAssets()
                ? uploadLiubaiTagAssets(context, productionPieceId, manufacturerMetaId, expandedWidth, expandedHeight)
                : null;
        updateTagMarks(piece, tagAssets);
        String originalContentImg = resolveOriginalContentImg(piece, originalMaskUrl);
        String expandedSvg = buildExpandedSvg(originalSvg, pieceMongoId, markPngUrl, tagAssets, originalContentImg, originalWidth, originalHeight, margins);
        String businessId = StringUtils.isNotBlank(piece.getProductionPieceId()) ? piece.getProductionPieceId() : pieceMongoId;
        String uploadPath = "mask/" + manufacturerMetaId + "/" + context.getOrderItem().getOrderItemId() + "/liubai/";
        String newMaskUrl = ossTagUploadService.uploadTagSvg(businessId, expandedSvg.getBytes(StandardCharsets.UTF_8), uploadPath);
        updateMaskImageFile(piece, newMaskUrl);
        piece.setWidth(expandedWidth);
        piece.setHeight(expandedHeight);
    }



    /**
     * 解析原始 mask 引用。默认读取 productionPiece.maskImageFile.rawFile；特殊工艺可以自行生成 SVG。
     */
    protected String resolveOriginalMaskRef(LiubaiProcessContext context, ProductionPiece piece) {
        if (piece == null || piece.getMaskImageFile() == null) {
            return null;
        }
        return piece.getMaskImageFile().getRawFile();
    }

    /**
     * 解析处理前 SVG 宽度，默认优先读取 SVG width，其次使用生产工件宽度。
     */
    protected double resolveOriginalWidth(LiubaiProcessContext context, String originalSvg, ProductionPiece piece) {
        return resolveDimension(originalSvg, SVG_WIDTH_PATTERN, piece == null ? null : piece.getWidth());
    }

    /**
     * 解析处理前 SVG 高度，默认优先读取 SVG height，其次使用生产工件高度。
     */
    protected double resolveOriginalHeight(LiubaiProcessContext context, String originalSvg, ProductionPiece piece) {
        return resolveDimension(originalSvg, SVG_HEIGHT_PATTERN, piece == null ? null : piece.getHeight());
    }

    /**
     * 当前实体策略的规格名称，用于生成外层 SVG 分组 id，便于排查最终 mask 来自哪套留白策略。
     *
     * @return 规格名称，例如 2cm 或 5cm
     */
    protected abstract String specName();

    /**
     * 当前实体策略的外扩毫米数。
     *
     * @return 外扩值，单位毫米
     */
    protected abstract double expandMm();

    /**
     * 当前实体策略可识别的关键字列表。
     *
     * @return 规格匹配关键字，例如 留白5cm、5cm、50mm、50毫米
     */
    protected abstract String[] matchKeywords();

    /**
     * 是否需要在留白 mark 的 SVG/PNG 中叠加原始尺寸灰色边框。
     *
     * <p>所有固定尺寸留白都需要保留原始尺寸参考线，因此默认启用。</p>
     *
     * @return 需要叠加原始尺寸边框时返回 {@code true}
     */
    protected boolean shouldDrawInnerOriginalBorder() {
        return true;
    }

    /**
     * 从最外侧留白矩形向内偏移多少毫米绘制虚线框。
     *
     * <p>默认不绘制；具体规格可按工艺要求返回正数。</p>
     *
     * @return 需要绘制虚线框时返回向内偏移毫米数，否则返回 0
     */
    protected double dashedInsetFromOuterBorderMm() {
        return 0D;
    }


    /**
     * 是否需要为当前工艺额外生成留白边缘文字标签。
     *
     * <p>常规留白需要按订单项和后续工艺生成贴边标签；只复用外框处理能力、
     * 不改变尺寸的工艺可以关闭该能力，避免产生额外 mark。</p>
     *
     * @return 需要生成边缘文字标签时返回 {@code true}
     */
    protected boolean shouldUploadLiubaiTagAssets() {
        return true;
    }

    /**
     * 确保生产工件已有业务生产工件 ID。
     *
     * <p>留白外扩矩形 PNG 需要上传到 mark/{manufacturerMetaId}/{productionPieceId}/ 目录。
     * 直接生成路线中，留白处理发生在 addProductionPiece 之前，因此 productionPieceId 可能尚未生成；
     * 这里复用生产工件领域服务原有的 PP 编号生成方法提前赋值，避免上传路径和后续入库 ID 不一致。</p>
     *
     * @param piece 当前生产工件
     * @return 生产工件业务 ID
     */
    private String ensureProductionPieceBusinessId(ProductionPiece piece) {
        if (StringUtils.isBlank(piece.getProductionPieceId())) {
            piece.setProductionPieceId(IdGenerator.generateId("PP"));
        }
        return piece.getProductionPieceId();
    }

    /**
     * 确保生产工件已有 MongoDB _id。
     *
     * <p>留白 SVG 的外层/内层 g id 需要使用 productionPiece 的 _id。直接生成路线中，
     * 留白处理发生在 addProductionPiece 之前，此时 _id 可能尚未由 MongoDB 生成；因此这里使用 MongoDB
     * 原生 ObjectId 生成方式提前分配一个 _id，保持与原先 MongoDB 自动生成 _id 一致的 24 位十六进制格式。</p>
     *
     * @param piece 当前生产工件
     * @return productionPiece 的 MongoDB _id
     */
    private String ensureProductionPieceMongoId(ProductionPiece piece) {
        if (StringUtils.isBlank(piece.getId())) {
            piece.setId(new ObjectId().toHexString());
        }
        return piece.getId();
    }

    /**
     * 生成并上传与外扩 SVG 同宽高的黑色边框矩形 PNG。
     *
     * <p>该 PNG 会作为 productionPiece.marks 中的留白 mark 保存，上传目录固定为
     * mark/{manufacturerMetaId}/{productionPieceId}/，便于后续排版/刀版流程按生产工件定位留白外框资源。</p>
     * <p>注意：外扩 SVG 宽高单位是 mm，PNG 实际像素宽高会按 36dpi 换算。</p>
     *
     * @param productionPieceId 生产工件业务 ID
     * @param manufacturerMetaId 厂商 ID
     * @param originalWidth 原始 SVG 宽度，单位 mm
     * @param originalHeight 原始 SVG 高度，单位 mm
     * @param margins 四边外扩量
     * @return 上传后的 PNG 完整 URL
     */
    private String uploadOuterRectMarkPng(String productionPieceId,
                                          String manufacturerMetaId,
                                          double originalWidth,
                                          double originalHeight,
                                          ExpandMargins margins) {
        String uploadPath = "mark/" + manufacturerMetaId + "/" + productionPieceId + "/";
        return ossTagUploadService.uploadTagPng(productionPieceId, createBorderPng(originalWidth, originalHeight, margins), uploadPath);
    }

    /**
     * 创建透明底留白边框 PNG。
     *
     * <p>所有固定留白都会生成外扩后画布的黑色边框和原始尺寸黑色边框；
     * 大于 5cm 的规格还会按工艺要求从最外框向内 5cm 绘制一圈黑色虚线框。</p>
     *
     * @param originalWidth 原始 SVG 宽度，单位 mm
     * @param originalHeight 原始 SVG 高度，单位 mm
     * @param margins 四边外扩量
     * @return PNG 文件字节数组
     */
    private byte[] createBorderPng(double originalWidth, double originalHeight, ExpandMargins margins) {
        double widthMm = originalWidth + margins.left + margins.right;
        double heightMm = originalHeight + margins.top + margins.bottom;
        int imageWidth = convertMmToPixels(widthMm);
        int imageHeight = convertMmToPixels(heightMm);
        try {
            BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            graphics.setStroke(new BasicStroke(borderStrokeWidthPx()));
            graphics.setColor(Color.BLACK);
            graphics.drawRect(0, 0, imageWidth - 1, imageHeight - 1);
            drawDashedInsetBorderIfNecessary(graphics, imageWidth, imageHeight, widthMm, heightMm, margins);
            drawInnerOriginalBorderIfNecessary(graphics, imageWidth, imageHeight, originalWidth, originalHeight, margins);
            graphics.dispose();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("生成留白外框 PNG 失败", e);
        }
    }

    private void drawInnerOriginalBorderIfNecessary(Graphics2D graphics,
                                                    int imageWidth,
                                                    int imageHeight,
                                                    double originalWidth,
                                                    double originalHeight,
                                                    ExpandMargins margins) {
        if (!shouldDrawInnerOriginalBorder()) {
            return;
        }
        drawPhysicalBorder(graphics, imageWidth, imageHeight, margins.left, margins.top, originalWidth, originalHeight, Color.BLACK, false);
    }

    private void drawDashedInsetBorderIfNecessary(Graphics2D graphics,
                                                   int imageWidth,
                                                   int imageHeight,
                                                   double widthMm,
                                                   double heightMm,
                                                   ExpandMargins margins) {
        double insetMm = dashedInsetFromOuterBorderMm();
        if (insetMm <= 0D) {
            return;
        }
        double leftInsetMm = dashedInsetForMargin(margins.left, insetMm);
        double topInsetMm = dashedInsetForMargin(margins.top, insetMm);
        double rightInsetMm = dashedInsetForMargin(margins.right, insetMm);
        double bottomInsetMm = dashedInsetForMargin(margins.bottom, insetMm);
        double innerWidthMm = widthMm - leftInsetMm - rightInsetMm;
        double innerHeightMm = heightMm - topInsetMm - bottomInsetMm;
        if (innerWidthMm <= 0D || innerHeightMm <= 0D) {
            return;
        }
        drawPhysicalBorder(graphics, imageWidth, imageHeight, leftInsetMm, topInsetMm, innerWidthMm, innerHeightMm, Color.BLACK, true);
    }

    private double dashedInsetForMargin(double marginMm, double insetMm) {
        return marginMm <= 0D ? 0D : insetMm;
    }

    private void drawPhysicalBorder(Graphics2D graphics,
                                    int imageWidth,
                                    int imageHeight,
                                    double leftMm,
                                    double topMm,
                                    double widthMm,
                                    double heightMm,
                                    Color color,
                                    boolean dashed) {
        int left = Math.min(imageWidth - 1, convertMmToPixels(leftMm));
        int top = Math.min(imageHeight - 1, convertMmToPixels(topMm));
        int right = Math.min(imageWidth - 1, convertMmToPixels(leftMm + widthMm) - 1);
        int bottom = Math.min(imageHeight - 1, convertMmToPixels(topMm + heightMm) - 1);
        if (right < left || bottom < top) {
            return;
        }
        graphics.setColor(color);
        graphics.setStroke(dashed
                ? new BasicStroke(borderStrokeWidthPx(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10F, new float[]{6F, 4F}, 0F)
                : new BasicStroke(borderStrokeWidthPx()));
        graphics.drawRect(left, top, right - left, bottom - top);
    }

    /**
     * 将毫米尺寸按照 36dpi 换算为像素。
     *
     * @param valueMm 毫米尺寸
     * @return 对应像素数，最小为 1
     */
    private int convertMmToPixels(double valueMm) {
        return Math.max(1, (int) Math.ceil(valueMm / MM_PER_INCH * MARK_PNG_DPI));
    }

    private int convertMmToPixels(double valueMm, double dpi) {
        return Math.max(1, (int) Math.ceil(valueMm / MM_PER_INCH * dpi));
    }


    /**
     * 留白标记边框线宽缩放比例。
     *
     * <p>默认保持历史线宽；具体规格策略可按工艺要求只放大线条本身，不改变边框位置或留白尺寸。</p>
     */
    protected double borderStrokeWidthScale() {
        return 1D;
    }

    private float borderStrokeWidthPx() {
        return (float) (DEFAULT_BORDER_STROKE_WIDTH_PX * borderStrokeWidthScale());
    }

    private String borderStrokeWidthSvg() {
        return format(DEFAULT_BORDER_STROKE_WIDTH_SVG * borderStrokeWidthScale());
    }


    /**
     * 生成并上传留白边缘文字标签。
     *
     * <p>标签内容由订单项 ID 后五位（元素 A）、生产图文件名（元素 B）、orderItem.material.usageSize3D 宽高尺寸和加工流程（元素 C）拼接而成。</p>
     * <p>标签与相邻边保持 1cm 间距；横向标签会按上下边可用长度从左往右截取，竖向标签会先按左右边可用长度截取横向文本后再旋转。</p>
     * <p>上边与右边标签朝向最外侧，避免文字方向朝向画面内部。</p>
     */
    private LiubaiTagAssets uploadLiubaiTagAssets(LiubaiProcessContext context, String productionPieceId, String manufacturerMetaId, double horizontalMaxWidthMm, double verticalMaxHeightMm) {
        String tagText = buildLiubaiTagText(context);
        if (StringUtils.isBlank(tagText)) {
            return null;
        }
        double horizontalAvailableWidthMm = availableLiubaiTagLength(horizontalMaxWidthMm);
        double verticalAvailableHeightMm = availableLiubaiTagLength(verticalMaxHeightMm);
        LiubaiTagImage horizontal = createHorizontalLiubaiTagPng(tagText);
        LiubaiTagImage bottom = cropHorizontalLiubaiTagPng(horizontal, horizontalAvailableWidthMm);
        LiubaiTagImage top = rotate180LiubaiTagPng(bottom);
        LiubaiTagImage left = createVerticalLiubaiTagPng(cropHorizontalLiubaiTagPng(horizontal, verticalAvailableHeightMm).image);
        LiubaiTagImage right = createOutwardRightLiubaiTagPng(cropHorizontalLiubaiTagPng(horizontal, verticalAvailableHeightMm).image);
        String uploadPath = "mark/" + manufacturerMetaId + "/" + productionPieceId + "/";
        String topUrl = ossTagUploadService.uploadTagPng(productionPieceId, top.bytes, uploadPath);
        String rightUrl = ossTagUploadService.uploadTagPng(productionPieceId, right.bytes, uploadPath);
        String bottomUrl = ossTagUploadService.uploadTagPng(productionPieceId, bottom.bytes, uploadPath);
        String leftUrl = ossTagUploadService.uploadTagPng(productionPieceId, left.bytes, uploadPath);
        return new LiubaiTagAssets(tagText, topUrl, rightUrl, bottomUrl, leftUrl, top.widthMm, top.heightMm, right.widthMm, right.heightMm, bottom.widthMm, bottom.heightMm, left.widthMm, left.heightMm);
    }

    private double availableLiubaiTagLength(double edgeLengthMm) {
        return Math.max(1D, edgeLengthMm - LIUBAI_TAG_ADJACENT_EDGE_GAP_MM * 2D);
    }

    protected String buildLiubaiTagText(LiubaiProcessContext context) {
        OrderItem orderItem = context == null ? null : context.getOrderItem();
        List<String> elements = new ArrayList<>();
        addIfNotBlank(elements, resolveOrderItemIdSuffix(context));
        addIfNotBlank(elements, resolveProductionImgFileName(orderItem));
        addIfNotBlank(elements, resolveMaterialUsageSizeText(orderItem));
        addIfNotBlank(elements, orderItem == null ? null : orderItem.getProcessingFlow());
        return String.join(" ", elements);
    }

    private void addIfNotBlank(List<String> values, String value) {
        if (StringUtils.isNotBlank(value)) {
            values.add(value.trim());
        }
    }

    private String resolveMaterialUsageSizeText(OrderItem orderItem) {
        if (orderItem == null || orderItem.getMaterial() == null || orderItem.getMaterial().getUsageSize3D() == null) {
            return "";
        }
        var usageSize3D = orderItem.getMaterial().getUsageSize3D();
        Number width = usageSize3D.getWidth();
        Number height = usageSize3D.getHeight();
        if (width == null || height == null) {
            return "";
        }
        return format(width.doubleValue()) + "*" + format(height.doubleValue());
    }

    private String resolveProductionImgFileName(OrderItem orderItem) {
        if (orderItem == null || orderItem.getProductionImgFile() == null) {
            return "";
        }
        try {
            Method getter = orderItem.getProductionImgFile().getClass().getMethod("getName");
            Object value = getter.invoke(orderItem.getProductionImgFile());
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private String resolveOrderItemIdSuffix(LiubaiProcessContext context) {
        if (context == null || context.getOrderItem() == null || StringUtils.isBlank(context.getOrderItem().getOrderItemId())) {
            return "";
        }
        String orderItemId = context.getOrderItem().getOrderItemId().trim();
        int startIndex = Math.max(0, orderItemId.length() - 5);
        return orderItemId.substring(startIndex);
    }

    private LiubaiTagImage createHorizontalLiubaiTagPng(String text) {
        double edgeSizeMm = liubaiTagEdgeSizeMm();
        int heightPx = convertMmToPixels(edgeSizeMm, LIUBAI_TAG_PNG_DPI);
        int fontSize = Math.max(12, (int) Math.floor(heightPx * 0.72D));
        Font font = new Font("SansSerif", Font.BOLD, fontSize);
        FontMetrics metrics = fontMetrics(font);
        int paddingPx = Math.max(4, convertMmToPixels(1D, LIUBAI_TAG_PNG_DPI));
        List<Color> colors = liubaiTagTextColors();
        if (colors == null || colors.isEmpty()) {
            colors = List.of(Color.BLACK);
        }
        int segmentGapPx = Math.max(paddingPx, convertMmToPixels(2D, LIUBAI_TAG_PNG_DPI));
        int textWidthPx = metrics.stringWidth(text);
        int widthPx = Math.max(1, textWidthPx * colors.size() + segmentGapPx * Math.max(0, colors.size() - 1) + paddingPx * 2);
        BufferedImage image = new BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        clearAndConfigureTextGraphics(graphics, widthPx, heightPx);
        graphics.setFont(font);
        FontMetrics imageMetrics = graphics.getFontMetrics();
        int x = paddingPx;
        int y = Math.max(imageMetrics.getAscent(), Math.min(heightPx - 1, (heightPx + imageMetrics.getAscent() - imageMetrics.getDescent()) / 2));
        for (Color color : colors) {
            graphics.setColor(color == null ? Color.BLACK : color);
            graphics.drawString(text, x, y);
            x += textWidthPx + segmentGapPx;
        }
        graphics.dispose();
        return new LiubaiTagImage(toPng(image), image, widthPx / LIUBAI_TAG_PNG_DPI * MM_PER_INCH, edgeSizeMm);
    }

    protected List<Color> liubaiTagTextColors() {
        return List.of(Color.BLACK);
    }

    /**
     * 留白后附加工艺标签贴边边长，单位 mm。
     *
     * <p>默认保持留白工艺历史 10mm；仅特殊工艺可覆盖该值，DPI 仍由 {@link #LIUBAI_TAG_PNG_DPI} 控制。</p>
     */
    protected double liubaiTagEdgeSizeMm() {
        return DEFAULT_LIUBAI_TAG_EDGE_SIZE_MM;
    }

    private LiubaiTagImage cropHorizontalLiubaiTagPng(LiubaiTagImage image, double maxWidthMm) {
        if (image == null || maxWidthMm <= 0D || image.widthMm <= maxWidthMm) {
            return image;
        }
        int cropWidthPx = Math.max(1, Math.min(image.image.getWidth(), convertMmToPixels(maxWidthMm, LIUBAI_TAG_PNG_DPI)));
        BufferedImage cropped = new BufferedImage(cropWidthPx, image.image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = cropped.createGraphics();
        clearAndConfigureTextGraphics(graphics, cropWidthPx, image.image.getHeight());
        graphics.drawImage(image.image, 0, 0, cropWidthPx, image.image.getHeight(), 0, 0, cropWidthPx, image.image.getHeight(), null);
        graphics.dispose();
        return new LiubaiTagImage(toPng(cropped), cropped, cropWidthPx / LIUBAI_TAG_PNG_DPI * MM_PER_INCH, image.heightMm);
    }

    private LiubaiTagImage rotate180LiubaiTagPng(LiubaiTagImage image) {
        BufferedImage rotated = rotate180(image.image);
        return new LiubaiTagImage(toPng(rotated), rotated, image.widthMm, image.heightMm);
    }

    private LiubaiTagImage createVerticalLiubaiTagPng(BufferedImage horizontalImage) {
        BufferedImage rotated = rotateClockwise90(horizontalImage);
        return new LiubaiTagImage(toPng(rotated), rotated, liubaiTagEdgeSizeMm(), rotated.getHeight() / LIUBAI_TAG_PNG_DPI * MM_PER_INCH);
    }

    private LiubaiTagImage createOutwardRightLiubaiTagPng(BufferedImage horizontalImage) {
        BufferedImage rotated = rotateCounterClockwise90(horizontalImage);
        return new LiubaiTagImage(toPng(rotated), rotated, liubaiTagEdgeSizeMm(), rotated.getHeight() / LIUBAI_TAG_PNG_DPI * MM_PER_INCH);
    }

    private FontMetrics fontMetrics(Font font) {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.dispose();
        return metrics;
    }

    private void clearAndConfigureTextGraphics(Graphics2D graphics, int width, int height) {
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(0, 0, width, height);
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private BufferedImage rotateClockwise90(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getHeight(), src.getWidth(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                dst.setRGB(src.getHeight() - 1 - y, x, src.getRGB(x, y));
            }
        }
        return dst;
    }

    private BufferedImage rotateCounterClockwise90(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getHeight(), src.getWidth(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                dst.setRGB(y, src.getWidth() - 1 - x, src.getRGB(x, y));
            }
        }
        return dst;
    }

    private BufferedImage rotate180(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                dst.setRGB(src.getWidth() - 1 - x, src.getHeight() - 1 - y, src.getRGB(x, y));
            }
        }
        return dst;
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
     * 根据当前生产工件的超幅拼接血边计算四边外扩量。
     *
     * <p>出血边约定：blood.x 或 blood.y 非 0 表示对应轴存在主动出血方向，正值/负值分别映射到该轴两侧边；
     * 同一拼接组内相邻零件被其他零件出血覆盖的“被出血边”也属于血边，需要按分片顺序一并跳过外扩。
     * 直接路线不跳过任何边，四边使用当前策略的固定留白尺寸。</p>
     *
     * @param piece 回调生产工件，包含 blood、group 和 seq 信息
     * @param skipBloodEdges 是否跳过出血边外扩
     * @return 四边最终外扩量
     */
    protected ExpandMargins resolveMargins(ProductionPiece piece, LiubaiProcessContext context) {
        double expandMm = expandMm();
        ExpandMargins margins = new ExpandMargins(expandMm, expandMm, expandMm, expandMm);
        boolean skipBloodEdges = context != null && context.isSkipBloodEdges();
        if (!skipBloodEdges || piece == null) {
            return margins;
        }
        Set<LiubaiEdge> bloodEdges = resolveBloodEdges(piece);
        if (bloodEdges.contains(LiubaiEdge.RIGHT)) {
            margins.right = 0D;
        }
        if (bloodEdges.contains(LiubaiEdge.LEFT)) {
            margins.left = 0D;
        }
        if (bloodEdges.contains(LiubaiEdge.TOP)) {
            margins.top = 0D;
        }
        if (bloodEdges.contains(LiubaiEdge.BOTTOM)) {
            margins.bottom = 0D;
        }
        return margins;
    }

    private Set<LiubaiEdge> resolveBloodEdges(ProductionPiece piece) {
        Set<LiubaiEdge> bloodEdges = EnumSet.noneOf(LiubaiEdge.class);
        Blood blood = piece.getBlood();
        addBloodEdgesFromBlood(bloodEdges, blood);
        bloodEdges.addAll(resolveBloodEdgesFromSequence(piece, blood));
        return bloodEdges;
    }

    private void addBloodEdgesFromBlood(Set<LiubaiEdge> bloodEdges, Blood blood) {
        if (blood == null) {
            return;
        }
        Integer x = blood.getX();
        Integer y = blood.getY();
        if (x != null && x > 0) {
            bloodEdges.add(LiubaiEdge.RIGHT);
        } else if (x != null && x < 0) {
            bloodEdges.add(LiubaiEdge.LEFT);
        }
        if (y != null && y > 0) {
            bloodEdges.add(LiubaiEdge.TOP);
        } else if (y != null && y < 0) {
            bloodEdges.add(LiubaiEdge.BOTTOM);
        }
    }

    /**
     * 按分片顺序推断被出血边。
     *
     * <p>blood.y 为 0 时代表竖切，分片沿左右方向相邻，按左右边补充；blood.x 为 0 时代表横切，
     * 分片沿上下方向相邻，需要按上下边补充，避免横切场景继续套用竖切的左右边逻辑。</p>
     */
    private Set<LiubaiEdge> resolveBloodEdgesFromSequence(ProductionPiece piece, Blood blood) {
        Set<LiubaiEdge> bloodEdges = EnumSet.noneOf(LiubaiEdge.class);
        Integer currentSeq = piece.getSeq();
        Integer maxSeq = extractMaxSeqInGroup(piece.getGroup());
        if (currentSeq == null || maxSeq == null || maxSeq <= 0) {
            return bloodEdges;
        }
        LiubaiEdge firstCoveredEdge = resolveFirstCoveredEdge(blood);
        LiubaiEdge lastCoveredEdge = oppositeEdge(firstCoveredEdge);
        if (currentSeq == 1 || (currentSeq > 1 && currentSeq < maxSeq)) {
            bloodEdges.add(firstCoveredEdge);
        }
        if (currentSeq.intValue() == maxSeq.intValue() || (currentSeq > 1 && currentSeq < maxSeq)) {
            bloodEdges.add(lastCoveredEdge);
        }
        return bloodEdges;
    }

    private LiubaiEdge resolveFirstCoveredEdge(Blood blood) {
        if (blood != null && isZero(blood.getX()) && isNonZero(blood.getY())) {
            return LiubaiEdge.BOTTOM;
        }
        return LiubaiEdge.RIGHT;
    }

    private LiubaiEdge oppositeEdge(LiubaiEdge edge) {
        switch (edge) {
            case BOTTOM:
                return LiubaiEdge.TOP;
            case TOP:
                return LiubaiEdge.BOTTOM;
            case LEFT:
                return LiubaiEdge.RIGHT;
            case RIGHT:
            default:
                return LiubaiEdge.LEFT;
        }
    }

    private boolean isZero(Integer value) {
        return value != null && value == 0;
    }

    private boolean isNonZero(Integer value) {
        return value != null && value != 0;
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

    /**
     * 解析原始 SVG 内容。
     *
     * <p>maskImageFile.rawFile 通常是 OSS URL；为了兼容调用方直接传入 SVG 字符串，
     * 这里先判断是否已经是 SVG/XML 内容，否则通过 HTTP 拉取远程文件。</p>
     *
     * @param svgRef SVG 字符串或远程 URL
     * @return SVG 文本内容
     */
    protected String resolveSvg(String svgRef) {
        String trimmed = svgRef.trim();
        if (trimmed.startsWith("<svg") || trimmed.startsWith("<?xml")) {
            return trimmed;
        }
        return restTemplate.getForObject(trimmed, String.class);
    }

    /**
     * 解析 SVG 宽高数值。
     *
     * @param svg SVG 文本
     * @param pattern 宽或高的正则表达式
     * @param fallback SVG 缺失对应属性时使用的兜底值
     * @return 解析后的尺寸，解析失败且无兜底值时返回 0
     */
    protected double resolveDimension(String svg, Pattern pattern, Double fallback) {
        Matcher matcher = pattern.matcher(svg);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return fallback == null ? 0D : fallback;
    }

    /**
     * 构建外扩后的新 mask SVG。
     *
     * <p>SVG 结构说明：</p>
     * <ul>
     *     <li>根节点宽高使用“原尺寸 + 四边外扩量”。</li>
     *     <li>viewBox 使用负 left/top 起点，让原 SVG 既有内容在视觉上落到留白区域内。</li>
     *     <li>原 SVG 已有 {@code <g>} 时不重建、不包裹、不移动原有 g，默认把留白矩形 g 插入为根节点首个子 g。</li>
     *     <li>原 SVG 没有 {@code <g>} 时重构为根节点下的分组结构，默认留白 g 在第一位，原内容包装为印版 g。</li>
     *     <li>只需同尺寸描边的工艺可重写插入顺序，让边框 g 追加到原内容之后，避免被原内容遮挡。</li>
     *     <li>留白矩形 g 的 id 格式为 liubai-{specName}-{productionPiece._id}，挂载对应 mark PNG 的 img、data-source-name、data-forme、data-rotation。</li>
     *     <li>留白矩形 g 内只放置外扩后的大矩形 path。</li>
     * </ul>
     *
     * @param originalSvg 原始 mask SVG 文本
     * @param pieceMongoId 当前生产工件 MongoDB _id，用于生成留白 g 的 id
     * @param markPngUrl 与外扩 SVG 同宽高的留白 mark PNG URL，用于写入留白矩形 g 的 img
     * @param originalContentImg 原始印版分组对应的 img 属性值
     * @param originalWidth 原始 SVG 宽度
     * @param originalHeight 原始 SVG 高度
     * @param margins 四边外扩量
     * @return 新生成的外扩 SVG 文本
     */
    private String buildExpandedSvg(String originalSvg,
                                    String pieceMongoId,
                                    String markPngUrl,
                                    LiubaiTagAssets tagAssets,
                                    String originalContentImg,
                                    double originalWidth,
                                    double originalHeight,
                                    ExpandMargins margins) {
        double newWidth = originalWidth + margins.left + margins.right;
        double newHeight = originalHeight + margins.top + margins.bottom;
        String updatedSvg = updateRootSvgAttributes(originalSvg, newWidth, newHeight, margins);
        String markSourceName = sourceName(markPngUrl);
        String liubaiGroup = buildLiubaiGroup(pieceMongoId, markPngUrl, markSourceName, originalWidth, originalHeight, margins);
        String tagGroups = buildLiubaiTagGroups(pieceMongoId, tagAssets, originalWidth, originalHeight, margins);
        String markGroups = liubaiGroup + tagGroups;
        if (!containsGroup(originalSvg)) {
            return rebuildSvgWithLiubaiGroups(updatedSvg, pieceMongoId, originalContentImg, markGroups);
        }
        return shouldInsertMarkGroupsBeforeOriginalContent()
                ? insertAfterRootOpenTag(updatedSvg, markGroups)
                : insertBeforeRootCloseTag(updatedSvg, markGroups);
    }

    private boolean containsGroup(String svg) {
        return StringUtils.isNotBlank(svg) && SVG_GROUP_PATTERN.matcher(svg).find();
    }

    private String insertAfterRootOpenTag(String svg, String groupSvg) {
        Matcher matcher = SVG_OPEN_PATTERN.matcher(svg);
        if (!matcher.find()) {
            return svg;
        }
        return svg.substring(0, matcher.end()) + groupSvg + svg.substring(matcher.end());
    }

    private String insertBeforeRootCloseTag(String svg, String groupSvg) {
        int closeIndex = lastSvgCloseIndex(svg);
        if (closeIndex < 0) {
            return svg;
        }
        return svg.substring(0, closeIndex) + groupSvg + svg.substring(closeIndex);
    }

    private String rebuildSvgWithLiubaiGroups(String updatedSvg, String pieceMongoId, String originalContentImg, String liubaiGroup) {
        Matcher openMatcher = SVG_OPEN_PATTERN.matcher(updatedSvg);
        if (!openMatcher.find()) {
            return updatedSvg;
        }
        int closeIndex = lastSvgCloseIndex(updatedSvg);
        if (closeIndex < 0 || closeIndex < openMatcher.end()) {
            return updatedSvg;
        }
        String prefix = updatedSvg.substring(0, openMatcher.end());
        String innerSvg = updatedSvg.substring(openMatcher.end(), closeIndex).trim();
        String suffix = updatedSvg.substring(closeIndex);
        String originalContentGroup = buildOriginalContentGroup(pieceMongoId, originalContentImg, innerSvg);
        return shouldInsertMarkGroupsBeforeOriginalContent()
                ? prefix + liubaiGroup + originalContentGroup + suffix
                : prefix + originalContentGroup + liubaiGroup + suffix;
    }

    private String buildOriginalContentGroup(String pieceMongoId, String originalContentImg, String innerSvg) {
        String groupId = StringUtils.isBlank(pieceMongoId) ? "original-mask" : pieceMongoId;
        return "<g id=\"" + escapeAttr(groupId)
                + "\" img=\"" + escapeAttr(originalContentImg)
                + "\" data-source-name=\"" + escapeAttr(sourceName(originalContentImg))
                + "\" data-rotation=\"0\">\n"
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

    protected String resolveImageFileRaw(ImageFile imageFile) {
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

    private int lastSvgCloseIndex(String svg) {
        Matcher matcher = SVG_CLOSE_PATTERN.matcher(svg);
        int closeIndex = -1;
        while (matcher.find()) {
            closeIndex = matcher.start();
        }
        return closeIndex;
    }

    private String updateRootSvgAttributes(String svg, double newWidth, double newHeight, ExpandMargins margins) {
        Matcher matcher = SVG_OPEN_PATTERN.matcher(svg);
        if (!matcher.find()) {
            return svg;
        }
        String openTag = matcher.group();
        String updatedOpenTag = upsertSvgAttribute(openTag, "width", format(newWidth));
        updatedOpenTag = upsertSvgAttribute(updatedOpenTag, "height", format(newHeight));
        updatedOpenTag = upsertSvgAttribute(updatedOpenTag, "viewBox",
                format(-margins.left) + " " + format(-margins.top) + " " + format(newWidth) + " " + format(newHeight));
        updatedOpenTag = upsertSvgAttribute(updatedOpenTag, "version", "1.1");
        updatedOpenTag = upsertSvgAttribute(updatedOpenTag, "require-plt", "true");
        return svg.substring(0, matcher.start()) + updatedOpenTag + svg.substring(matcher.end());
    }

    private String upsertSvgAttribute(String tag, String name, String value) {
        Pattern attributePattern = Pattern.compile("\\s" + Pattern.quote(name) + "\\s*=\\s*([\"']).*?\\1", Pattern.CASE_INSENSITIVE);
        Matcher matcher = attributePattern.matcher(tag);
        String attribute = " " + name + "=\"" + escapeAttr(value) + "\"";
        if (matcher.find()) {
            return matcher.replaceFirst(Matcher.quoteReplacement(attribute));
        }
        int insertIndex = tag.endsWith("/>") ? tag.length() - 2 : tag.length() - 1;
        return tag.substring(0, insertIndex) + attribute + tag.substring(insertIndex);
    }

    /**
     * mark 分组是否插入在原始内容之前。
     *
     * <p>固定尺寸留白默认保持历史行为，把外扩矩形放在原内容下方；
     * 零外扩的描边工艺需要放到原内容之后，否则同尺寸边框会被原始矩形遮挡。</p>
     */
    protected boolean shouldInsertMarkGroupsBeforeOriginalContent() {
        return true;
    }

    /**
     * 留白外框 path 的填充属性。
     *
     * <p>固定尺寸留白默认保持历史半透明填充；只需要黑色描边的工艺可返回 {@code fill="none"}。</p>
     */
    protected String outerRectFillAttributes() {
        return "fill=\"#d1495b\" fill-opacity=\"0.82\"";
    }

    private String buildLiubaiGroup(String pieceMongoId,
                                    String markPngUrl,
                                    String markSourceName,
                                    double originalWidth,
                                    double originalHeight,
                                    ExpandMargins margins) {
        double left = -margins.left;
        double top = -margins.top;
        double right = originalWidth + margins.right;
        double bottom = originalHeight + margins.bottom;
        return "\n<g id=\"liubai-" + specName() + "-" + escapeAttr(pieceMongoId) + "\" img=\"" + escapeAttr(markPngUrl)
                + "\" data-source-name=\"" + escapeAttr(markSourceName) + "\" data-forme=\"false\" data-rotation=\"0\" require-plt=\"false\">\n"
                + "<path d=\"M" + format(left) + " " + format(top) + " H" + format(right) + " V" + format(bottom)
                + " H" + format(left) + " Z\" " + outerRectFillAttributes() + " stroke=\"#111111\" stroke-width=\""
                + borderStrokeWidthSvg() + "\" fill-rule=\"evenodd\" />\n"
                + buildDashedInsetBorderPath(left, top, right, bottom, margins)
                + buildInnerOriginalBorderPath(originalWidth, originalHeight)
                + "</g>\n";
    }

    private String buildLiubaiTagGroups(String pieceMongoId,
                                         LiubaiTagAssets tagAssets,
                                         double originalWidth,
                                         double originalHeight,
                                         ExpandMargins margins) {
        if (tagAssets == null) {
            return "";
        }
        double left = -margins.left;
        double top = -margins.top;
        double right = originalWidth + margins.right;
        double bottom = originalHeight + margins.bottom;
        StringBuilder builder = new StringBuilder();
        double horizontalX = left + LIUBAI_TAG_ADJACENT_EDGE_GAP_MM;
        double verticalY = top + LIUBAI_TAG_ADJACENT_EDGE_GAP_MM;
        appendLiubaiTagGroup(builder, pieceMongoId, "horizontal", "top", tagAssets.topUrl, tagAssets.topWidthMm, tagAssets.topHeightMm, horizontalX, top);
        appendLiubaiTagGroup(builder, pieceMongoId, "horizontal", "bottom", tagAssets.bottomUrl, tagAssets.bottomWidthMm, tagAssets.bottomHeightMm, horizontalX, bottom - tagAssets.bottomHeightMm);
        appendLiubaiTagGroup(builder, pieceMongoId, "vertical", "left", tagAssets.leftUrl, tagAssets.leftWidthMm, tagAssets.leftHeightMm, left, verticalY);
        appendLiubaiTagGroup(builder, pieceMongoId, "vertical", "right", tagAssets.rightUrl, tagAssets.rightWidthMm, tagAssets.rightHeightMm, right - tagAssets.rightWidthMm, verticalY);
        return builder.toString();
    }


    private void appendLiubaiTagGroup(StringBuilder builder,
                                      String pieceMongoId,
                                      String direction,
                                      String suffix,
                                      String imageUrl,
                                      double widthMm,
                                      double heightMm,
                                      double x,
                                      double y) {
        builder.append("<g id=\"liubai-tag-").append(direction).append("-").append(suffix).append("-").append(escapeAttr(pieceMongoId))
                .append("\" img=\"").append(escapeAttr(imageUrl))
                .append("\" data-source-name=\"").append(escapeAttr(sourceName(imageUrl)))
                .append("\" data-forme=\"false\" data-rotation=\"0\" require-plt=\"false\" transform=\"translate(").append(format(x)).append(" ").append(format(y)).append(")\">\n")
                .append("<path d=\"M0 0 H").append(format(widthMm)).append(" V").append(format(heightMm)).append(" H0 Z\" fill=\"#999999\" />\n")
                .append("</g>\n");
    }

    private String buildDashedInsetBorderPath(double outerLeft,
                                              double outerTop,
                                              double outerRight,
                                              double outerBottom,
                                              ExpandMargins margins) {
        double insetMm = dashedInsetFromOuterBorderMm();
        if (insetMm <= 0D) {
            return "";
        }
        double left = outerLeft + dashedInsetForMargin(margins.left, insetMm);
        double top = outerTop + dashedInsetForMargin(margins.top, insetMm);
        double right = outerRight - dashedInsetForMargin(margins.right, insetMm);
        double bottom = outerBottom - dashedInsetForMargin(margins.bottom, insetMm);
        if (right <= left || bottom <= top) {
            return "";
        }
        return "<path d=\"M" + format(left) + " " + format(top) + " H" + format(right) + " V" + format(bottom)
                + " H" + format(left) + " Z\" fill=\"none\" stroke=\"#111111\" stroke-width=\""
                + borderStrokeWidthSvg() + "\" stroke-dasharray=\"6 4\" fill-rule=\"evenodd\" />\n";
    }

    private String buildInnerOriginalBorderPath(double originalWidth, double originalHeight) {
        if (!shouldDrawInnerOriginalBorder()) {
            return "";
        }
        return "<path d=\"M0 0 H" + format(originalWidth) + " V" + format(originalHeight)
                + " H0 Z\" fill=\"none\" stroke=\"#111111\" stroke-width=\"" + borderStrokeWidthSvg() + "\" fill-rule=\"evenodd\" />\n";
    }

    /**
     * 从 URL 中提取文件名作为 data-source-name。
     *
     * @param url 原始 mask SVG URL
     * @return 去除 query string 后的文件名
     */
    private String sourceName(String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        int queryIndex = url.indexOf('?');
        String clean = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
        int slashIndex = clean.lastIndexOf('/');
        return slashIndex >= 0 ? clean.substring(slashIndex + 1) : clean;
    }

    /**
     * 转义 XML 属性值，避免 URL 或 ID 中的特殊字符破坏 SVG 结构。
     *
     * @param value 原始属性值
     * @return 可安全写入 XML 属性的值
     */
    protected String escapeAttr(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * 格式化 SVG 数值。
     *
     * <p>整数不保留小数；非整数最多保留 4 位小数并去除尾随 0，减少 SVG 文本噪音。</p>
     *
     * @param value 原始数值
     * @return 格式化后的 SVG 数值字符串
     */
    protected String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.000001D) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    /**
     * 将新上传的留白 mark PNG 地址回写到生产工件 marks。
     *
     * <p>productionPiece.marks 的结构参考 TypesettingInfo.marks：key 表示 mark 类型，value 表示 OSS 地址。
     * 留白只会为当前生产工件保存一个外扩矩形 mark，因此使用 liubai-{specName} 作为 key，
     * 后续流程可以按该 key 读取对应的留白外框 PNG。</p>
     *
     * @param piece 当前生产工件
     * @param markUrl 新上传的留白 mark PNG URL
     */
    private void updateMarks(ProductionPiece piece, String markUrl) {
        Map<String, String> marks = piece.getMarks();
        if (marks == null) {
            marks = new LinkedHashMap<>();
            piece.setMarks(marks);
        }
        marks.put(liubaiMarkKey(), markUrl);
    }

    /**
     * 生成 productionPiece.marks 中保存留白 mark 的 key。
     *
     * <p>key 只表达 mark 类型与留白规格，不携带 productionPieceId；productionPiece 本身已经限定了资源归属。</p>
     *
     * @return 留白 mark key，例如 liubai-2cm 或 liubai-5cm
     */
    private String liubaiMarkKey() {
        return "liubai-" + specName();
    }

    /**
     * 将新上传的 mask SVG 地址回写到生产工件。
     *
     * <p>后续排版优先读取 productionPiece.maskImageFile.rawFile，因此 rawFile、preview.raw、preview、thumbnail
     * 都统一指向新生成的留白 SVG 地址。</p>
     *
     * @param piece 当前生产工件
     * @param maskUrl 新上传的留白 mask SVG URL
     */
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

    private void updateTagMarks(ProductionPiece piece, LiubaiTagAssets tagAssets) {
        if (tagAssets == null) {
            return;
        }
        Map<String, String> marks = piece.getMarks();
        if (marks == null) {
            marks = new LinkedHashMap<>();
            piece.setMarks(marks);
        }
        marks.put("liubai-tag-top", tagAssets.topUrl);
        marks.put("liubai-tag-right", tagAssets.rightUrl);
        marks.put("liubai-tag-bottom", tagAssets.bottomUrl);
        marks.put("liubai-tag-left", tagAssets.leftUrl);
    }

    private static class LiubaiTagImage {
        private final byte[] bytes;
        private final BufferedImage image;
        private final double widthMm;
        private final double heightMm;

        private LiubaiTagImage(byte[] bytes, BufferedImage image, double widthMm, double heightMm) {
            this.bytes = bytes;
            this.image = image;
            this.widthMm = widthMm;
            this.heightMm = heightMm;
        }
    }

    private static class LiubaiTagAssets {
        private final String text;
        private final String topUrl;
        private final String rightUrl;
        private final String bottomUrl;
        private final String leftUrl;
        private final double topWidthMm;
        private final double topHeightMm;
        private final double rightWidthMm;
        private final double rightHeightMm;
        private final double bottomWidthMm;
        private final double bottomHeightMm;
        private final double leftWidthMm;
        private final double leftHeightMm;

        private LiubaiTagAssets(String text, String topUrl, String rightUrl, String bottomUrl, String leftUrl,
                                double topWidthMm, double topHeightMm, double rightWidthMm, double rightHeightMm,
                                double bottomWidthMm, double bottomHeightMm, double leftWidthMm, double leftHeightMm) {
            this.text = text;
            this.topUrl = topUrl;
            this.rightUrl = rightUrl;
            this.bottomUrl = bottomUrl;
            this.leftUrl = leftUrl;
            this.topWidthMm = topWidthMm;
            this.topHeightMm = topHeightMm;
            this.rightWidthMm = rightWidthMm;
            this.rightHeightMm = rightHeightMm;
            this.bottomWidthMm = bottomWidthMm;
            this.bottomHeightMm = bottomHeightMm;
            this.leftWidthMm = leftWidthMm;
            this.leftHeightMm = leftHeightMm;
        }
    }

    private enum LiubaiEdge {
        TOP,
        RIGHT,
        BOTTOM,
        LEFT
    }

    /**
     * 四边外扩量值对象。
     *
     * <p>单位为毫米；callback 路线会根据出血边把其中某些边置为 0。</p>
     */
    protected static class ExpandMargins {
        /** 左边外扩量。 */
        protected double left;
        /** 右边外扩量。 */
        protected double right;
        /** 上边外扩量。 */
        protected double top;
        /** 下边外扩量。 */
        protected double bottom;

        /**
         * 构造四边外扩量。
         *
         * @param left 左边外扩量
         * @param right 右边外扩量
         * @param top 上边外扩量
         * @param bottom 下边外扩量
         */
        protected ExpandMargins(double left, double right, double top, double bottom) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }
    }
}

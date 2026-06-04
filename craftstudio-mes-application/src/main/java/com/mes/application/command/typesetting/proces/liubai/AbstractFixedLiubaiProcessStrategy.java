package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.shared.utils.IdGenerator;
import com.mes.domain.manufacturer.productionPiece.entity.Blood;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.piliofpala.craftstudio.shared.domain.file.vo.FilePreview;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import io.micrometer.common.util.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 固定尺寸留白实体策略基类。
 *
 * <p>设计目的：</p>
 * <ul>
 *     <li>“留白3cm”“留白5cm”等固定尺寸留白的处理流程完全一致，仅匹配关键字和外扩毫米数不同。</li>
 *     <li>该基类集中实现 SVG 拉取、尺寸解析、非出血边外扩、SVG 生成、mark PNG 生成、OSS 上传与工件字段回写。</li>
 *     <li>具体规格策略只需要提供规格名称、外扩毫米数和匹配关键字，避免多套留白策略复制同一份 SVG 处理逻辑。</li>
 * </ul>
 */
public abstract class AbstractFixedLiubaiProcessStrategy extends AbstractLiubaiProcessStrategy {
    /**
     * PNG 标记图使用的输出 DPI。
     *
     * <p>外扩 SVG 的宽高单位按业务约定为 mm，生成 PNG 时需要按照 36dpi 将 mm 换算成像素。</p>
     */
    private static final double MARK_PNG_DPI = 36D;

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
    private static final Pattern LIUBAI_SVG_ROOT_OPEN_PATTERN = Pattern.compile("<svg\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    /**
     * 判断原 SVG 是否已经包含分组。没有分组时需要重写 SVG，把原始图形放入新 g。
     */
    private static final Pattern LIUBAI_SVG_GROUP_PATTERN = Pattern.compile("<g\\b", Pattern.CASE_INSENSITIVE);

    /**
     * 提取原 SVG 根节点内部内容，用于无分组 SVG 重写时保留原始图形。
     */
    private static final Pattern SVG_INNER_PATTERN = Pattern.compile("<svg\\b[^>]*>([\\s\\S]*?)</svg>", Pattern.CASE_INSENSITIVE);

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
     * <p>具体策略通过 {@link #matchKeywords()} 提供规格关键字，例如 3cm / 30mm 或 5cm / 50mm。</p>
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
     *     <li>如果原 SVG 已有 g，则只更新根节点尺寸/viewBox，并把留白矩形 g 插入为第一个 g。</li>
     *     <li>如果原 SVG 没有 g，则重写 SVG，将原始图形放入原图 g，并生成留白矩形 g。</li>
     *     <li>上传新 SVG，回写 productionPiece.maskImageFile，并更新生产工件宽高。</li>
     * </ol>
     *
     * @param context 留白处理上下文
     */
    @Override
    public void process(LiubaiProcessContext context) {
        ProductionPiece piece = context.getProductionPiece();
        if (piece == null || piece.getMaskImageFile() == null || StringUtils.isBlank(piece.getMaskImageFile().getRawFile())) {
            return;
        }
        String originalMaskUrl = piece.getMaskImageFile().getRawFile();
        String originalSvg = resolveSvg(originalMaskUrl);
        if (StringUtils.isBlank(originalSvg)) {
            return;
        }
        double originalWidth = resolveDimension(originalSvg, SVG_WIDTH_PATTERN, piece.getWidth());
        double originalHeight = resolveDimension(originalSvg, SVG_HEIGHT_PATTERN, piece.getHeight());
        if (originalWidth <= 0 || originalHeight <= 0) {
            return;
        }

        ExpandMargins margins = resolveMargins(piece.getBlood(), context.isSkipBloodEdges());
        String pieceMongoId = ensureProductionPieceMongoId(piece);
        String productionPieceId = ensureProductionPieceBusinessId(piece);
        String manufacturerMetaId = StringUtils.isBlank(piece.getManufacturerId()) ? "default" : piece.getManufacturerId();
        String markPngUrl = uploadOuterRectMarkPng(productionPieceId, manufacturerMetaId, originalWidth, originalHeight, margins);
        updateMarks(piece, markPngUrl);
        String expandedSvg = buildExpandedSvg(originalSvg, originalMaskUrl, piece, pieceMongoId, markPngUrl, originalWidth, originalHeight, margins);
        String businessId = StringUtils.isNotBlank(piece.getProductionPieceId()) ? piece.getProductionPieceId() : pieceMongoId;
        String uploadPath = "mask/" + manufacturerMetaId + "/" + context.getOrderItem().getOrderItemId() + "/liubai/";
        String newMaskUrl = ossTagUploadService.uploadTagSvg(businessId, expandedSvg.getBytes(StandardCharsets.UTF_8), uploadPath);
        updateMaskImageFile(piece, newMaskUrl);
        piece.setWidth(originalWidth + margins.left + margins.right);
        piece.setHeight(originalHeight + margins.top + margins.bottom);
    }

    /**
     * 当前实体策略的规格名称，用于生成外层 SVG 分组 id，便于排查最终 mask 来自哪套留白策略。
     *
     * @return 规格名称，例如 3cm 或 5cm
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
        double newWidth = originalWidth + margins.left + margins.right;
        double newHeight = originalHeight + margins.top + margins.bottom;
        String uploadPath = "mark/" + manufacturerMetaId + "/" + productionPieceId + "/";
        return ossTagUploadService.uploadTagPng(productionPieceId, createBorderPng(newWidth, newHeight), uploadPath);
    }

    /**
     * 创建透明底黑色边框矩形 PNG。
     *
     * @param widthMm PNG 对应的物理宽度，单位 mm
     * @param heightMm PNG 对应的物理高度，单位 mm
     * @return PNG 文件字节数组
     */
    private byte[] createBorderPng(double widthMm, double heightMm) {
        int imageWidth = convertMmToPixels(widthMm);
        int imageHeight = convertMmToPixels(heightMm);
        try {
            BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.BLACK);
            graphics.setStroke(new BasicStroke(1F));
            graphics.drawRect(0, 0, imageWidth - 1, imageHeight - 1);
            graphics.dispose();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("生成留白外框 PNG 失败", e);
        }
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

    /**
     * 根据 blood 信息计算四边外扩量。
     *
     * <p>出血边约定：blood.x 或 blood.y 非 0 表示对应轴存在出血方向。
     * 在 callback 路线中，正值/负值分别映射到该轴两侧边，用于跳过出血边外扩；
     * 直接路线不跳过任何边，四边使用当前策略的固定留白尺寸。</p>
     *
     * @param blood 回调生产工件上的出血信息
     * @param skipBloodEdges 是否跳过出血边外扩
     * @return 四边最终外扩量
     */
    private ExpandMargins resolveMargins(Blood blood, boolean skipBloodEdges) {
        double expandMm = expandMm();
        ExpandMargins margins = new ExpandMargins(expandMm, expandMm, expandMm, expandMm);
        if (!skipBloodEdges || blood == null) {
            return margins;
        }
        Integer x = blood.getX();
        Integer y = blood.getY();
        if (x != null && x > 0) {
            margins.right = 0D;
        } else if (x != null && x < 0) {
            margins.left = 0D;
        }
        if (y != null && y > 0) {
            margins.top = 0D;
        } else if (y != null && y < 0) {
            margins.bottom = 0D;
        }
        return margins;
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
    private String resolveSvg(String svgRef) {
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
    private double resolveDimension(String svg, Pattern pattern, Double fallback) {
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
     *     <li>原 SVG 已有 g 时，只更新根节点 viewBox，并把留白矩形 g 插入到根节点下第一位，避免覆盖四角打扣等已写入分组。</li>
     *     <li>原 SVG 没有 g 时，重写 SVG：根节点下生成留白矩形 g 和包裹原始图形的原图 g。</li>
     *     <li>留白矩形 g 的 id 格式为 liubai-{specName}-{productionPiece._id}，挂载对应 mark PNG 的 img、data-source-name、data-forme、data-rotation。</li>
     * </ul>
     *
     * @param originalSvg 原始 mask SVG 文本
     * @param originalMaskUrl 原始 mask SVG 地址，用于无 g 重写时写入 data-source-name
     * @param piece 当前生产工件，用于无 g 重写时读取图片地址
     * @param pieceMongoId 当前生产工件 MongoDB _id，用于生成留白 g 的 id
     * @param markPngUrl 与外扩 SVG 同宽高的留白 mark PNG URL，用于写入留白矩形 g 的 img
     * @param originalWidth 原始 SVG 宽度
     * @param originalHeight 原始 SVG 高度
     * @param margins 四边外扩量
     * @return 新生成的外扩 SVG 文本
     */
    private String buildExpandedSvg(String originalSvg,
                                    String originalMaskUrl,
                                    ProductionPiece piece,
                                    String pieceMongoId,
                                    String markPngUrl,
                                    double originalWidth,
                                    double originalHeight,
                                    ExpandMargins margins) {
        double newWidth = originalWidth + margins.left + margins.right;
        double newHeight = originalHeight + margins.top + margins.bottom;
        String markSourceName = sourceName(markPngUrl);
        if (!hasGroup(originalSvg)) {
            return rebuildExpandedSvg(originalSvg, originalMaskUrl, piece, pieceMongoId, markPngUrl, markSourceName,
                    originalWidth, originalHeight, margins, newWidth, newHeight);
        }
        String updatedSvg = updateRootSvgAttributes(originalSvg, newWidth, newHeight, margins);
        String liubaiGroup = buildOffsetLiubaiRectGroup(pieceMongoId, markPngUrl, markSourceName, originalWidth, originalHeight, margins);
        Matcher matcher = LIUBAI_SVG_ROOT_OPEN_PATTERN.matcher(updatedSvg);
        if (!matcher.find()) {
            return updatedSvg;
        }
        return updatedSvg.substring(0, matcher.end()) + liubaiGroup + updatedSvg.substring(matcher.end());
    }

    private boolean hasGroup(String svg) {
        return LIUBAI_SVG_GROUP_PATTERN.matcher(svg).find();
    }

    private String updateRootSvgAttributes(String svg, double newWidth, double newHeight, ExpandMargins margins) {
        Matcher matcher = LIUBAI_SVG_ROOT_OPEN_PATTERN.matcher(svg);
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

    private String rebuildExpandedSvg(String originalSvg,
                                      String originalMaskUrl,
                                      ProductionPiece piece,
                                      String pieceMongoId,
                                      String markPngUrl,
                                      String markSourceName,
                                      double originalWidth,
                                      double originalHeight,
                                      ExpandMargins margins,
                                      double newWidth,
                                      double newHeight) {
        String inner = extractInnerSvg(originalSvg);
        String productImg = piece.getProductImageFile() == null ? "" : piece.getProductImageFile().getRawFile();
        String sourceName = sourceName(originalMaskUrl);
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + format(newWidth) + "\" height=\"" + format(newHeight)
                + "\" viewBox=\"0 0 " + format(newWidth) + " " + format(newHeight) + "\" version=\"1.1\" require-plt=\"true\">"
                + buildTranslatedLiubaiRectGroup(pieceMongoId, markPngUrl, markSourceName, originalWidth, originalHeight, margins)
                + "<g id=\"" + escapeAttr(pieceMongoId) + "\" img=\"" + escapeAttr(productImg)
                + "\" data-source-name=\"" + escapeAttr(sourceName) + "\" data-forme=\"false\" data-rotation=\"0\" transform=\"translate("
                + format(margins.left) + " " + format(margins.top) + ")\">\n"
                + inner + "\n"
                + "</g>\n"
                + "</svg>";
    }

    private String extractInnerSvg(String svg) {
        String withoutXml = svg.replaceFirst("(?is)^\\s*<\\?xml[^>]*>\\s*", "");
        Matcher matcher = SVG_INNER_PATTERN.matcher(withoutXml);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return withoutXml.trim();
    }

    private String buildOffsetLiubaiRectGroup(String pieceMongoId,
                                           String markPngUrl,
                                           String markSourceName,
                                           double originalWidth,
                                           double originalHeight,
                                           ExpandMargins margins) {
        return composeLiubaiRectGroup(pieceMongoId, markPngUrl, markSourceName,
                -margins.left, -margins.top, originalWidth + margins.right, originalHeight + margins.bottom);
    }

    private String buildTranslatedLiubaiRectGroup(String pieceMongoId,
                                              String markPngUrl,
                                              String markSourceName,
                                              double originalWidth,
                                              double originalHeight,
                                              ExpandMargins margins) {
        return composeLiubaiRectGroup(pieceMongoId, markPngUrl, markSourceName,
                0D, 0D, originalWidth + margins.left + margins.right, originalHeight + margins.top + margins.bottom);
    }

    private String composeLiubaiRectGroup(String pieceMongoId,
                                    String markPngUrl,
                                    String markSourceName,
                                    double left,
                                    double top,
                                    double right,
                                    double bottom) {
        return "\n<g id=\"liubai-" + specName() + "-" + escapeAttr(pieceMongoId) + "\" img=\"" + escapeAttr(markPngUrl)
                + "\" data-source-name=\"" + escapeAttr(markSourceName) + "\" data-forme=\"false\" data-rotation=\"0\">\n"
                + "<path d=\"M" + format(left) + " " + format(top) + " H" + format(right) + " V" + format(bottom)
                + " H" + format(left) + " Z\" fill=\"#d1495b\" fill-opacity=\"0.82\" stroke=\"#111111\" stroke-width=\"1.23\" fill-rule=\"evenodd\" />\n"
                + "</g>\n";
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
    private String escapeAttr(String value) {
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
    private String format(double value) {
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
     * @return 留白 mark key，例如 liubai-3cm 或 liubai-5cm
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

    /**
     * 四边外扩量值对象。
     *
     * <p>单位为毫米；callback 路线会根据出血边把其中某些边置为 0。</p>
     */
    private static class ExpandMargins {
        /** 左边外扩量。 */
        private double left;
        /** 右边外扩量。 */
        private double right;
        /** 上边外扩量。 */
        private double top;
        /** 下边外扩量。 */
        private double bottom;

        /**
         * 构造四边外扩量。
         *
         * @param left 左边外扩量
         * @param right 右边外扩量
         * @param top 上边外扩量
         * @param bottom 下边外扩量
         */
        private ExpandMargins(double left, double right, double top, double bottom) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }
    }
}

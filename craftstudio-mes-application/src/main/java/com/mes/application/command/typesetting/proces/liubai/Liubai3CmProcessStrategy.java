package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.productionPiece.entity.Blood;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.piliofpala.craftstudio.shared.domain.file.vo.FilePreview;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * “留白3cm”实体策略。
 *
 * <p>业务规则：</p>
 * <ul>
 *     <li>留白3cm等价于在矩形 mask 外侧增加 30mm 留白区域。</li>
 *     <li>无超幅拼接的直接路线：四边都外扩 30mm。</li>
 *     <li>存在超幅拼接的 callback 路线：只外扩非出血边，出血边根据 productionPiece.blood 判断后保持原尺寸。</li>
 *     <li>生成的新 SVG 会作为 productionPiece.maskImageFile.rawFile 回写，并同步更新生产工件宽高。</li>
 * </ul>
 */
@Service
public class Liubai3CmProcessStrategy extends AbstractLiubaiProcessStrategy {
    /**
     * 留白3cm对应的毫米外扩值。
     */
    private static final double EXPAND_MM = 30D;

    /**
     * 从 SVG width 属性中解析数值，兼容纯数字、px、mm 三种写法。
     */
    private static final Pattern SVG_WIDTH_PATTERN = Pattern.compile("width\\s*=\\s*[\"']\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:px|mm)?\\s*[\"']", Pattern.CASE_INSENSITIVE);
    /**
     * 从 SVG height 属性中解析数值，兼容纯数字、px、mm 三种写法。
     */
    private static final Pattern SVG_HEIGHT_PATTERN = Pattern.compile("height\\s*=\\s*[\"']\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:px|mm)?\\s*[\"']", Pattern.CASE_INSENSITIVE);
    /**
     * 提取原 SVG 根节点内部内容，用于包入新生成的外扩 SVG 内层 g。
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
     * 构造留白3cm实体策略。
     *
     * @param restTemplate 远程 SVG 拉取客户端
     * @param ossTagUploadService SVG 上传服务
     */
    public Liubai3CmProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        this.restTemplate = restTemplate;
        this.ossTagUploadService = ossTagUploadService;
    }

    /**
     * 判断工艺流程是否命中“留白3cm”。
     *
     * <p>兼容说明：产品配置可能使用厘米或毫米作为展示单位，也可能把规格放在节点名或参数对象文本中，
     * 因此这里同时匹配 3cm、3厘米、30mm、30毫米 等关键字。</p>
     *
     * @param procedureFlow 已解析工艺流程
     * @return {@code true} 表示命中留白3cm实体策略
     */
    @Override
    protected boolean matchesLiubaiValue(ProcedureFlow procedureFlow) {
        return containsInNodeOrParams(procedureFlow, "留白3cm")
                || containsInNodeOrParams(procedureFlow, "留白3CM")
                || containsInNodeOrParams(procedureFlow, "留白3厘米")
                || containsInNodeOrParams(procedureFlow, "留白30mm")
                || containsInNodeOrParams(procedureFlow, "留白30毫米")
                || containsInNodeOrParams(procedureFlow, "3cm")
                || containsInNodeOrParams(procedureFlow, "30mm")
                || containsInNodeOrParams(procedureFlow, "30毫米");
    }

    /**
     * 执行留白3cm外扩处理。
     *
     * <p>处理步骤：</p>
     * <ol>
     *     <li>读取当前 productionPiece.maskImageFile.rawFile 指向的原始 mask SVG。</li>
     *     <li>解析原始 SVG 宽高，解析失败时退回使用 productionPiece.width/height。</li>
     *     <li>根据是否需要跳过出血边计算四边外扩量。</li>
     *     <li>生成外层大矩形 SVG，并把原 SVG 内容包入内层 g。</li>
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
        String expandedSvg = buildExpandedSvg(originalSvg, originalMaskUrl, piece, originalWidth, originalHeight, margins);
        String manufacturerMetaId = StringUtils.isBlank(piece.getManufacturerId()) ? "default" : piece.getManufacturerId();
        String businessId = StringUtils.isNotBlank(piece.getProductionPieceId()) ? piece.getProductionPieceId() : context.getOrderItem().getOrderItemId();
        String uploadPath = "mask/" + manufacturerMetaId + "/" + context.getOrderItem().getOrderItemId() + "/liubai/";
        String newMaskUrl = ossTagUploadService.uploadTagSvg(businessId, expandedSvg.getBytes(StandardCharsets.UTF_8), uploadPath);
        updateMaskImageFile(piece, newMaskUrl);
        piece.setWidth(originalWidth + margins.left + margins.right);
        piece.setHeight(originalHeight + margins.top + margins.bottom);
    }

    /**
     * 根据 blood 信息计算四边外扩量。
     *
     * <p>出血边约定：blood.x 或 blood.y 非 0 表示对应轴存在出血方向。
     * 在 callback 路线中，正值/负值分别映射到该轴两侧边，用于跳过出血边外扩；
     * 直接路线不跳过任何边，四边固定 30mm。</p>
     *
     * @param blood 回调生产工件上的出血信息
     * @param skipBloodEdges 是否跳过出血边外扩
     * @return 四边最终外扩量
     */
    private ExpandMargins resolveMargins(Blood blood, boolean skipBloodEdges) {
        ExpandMargins margins = new ExpandMargins(EXPAND_MM, EXPAND_MM, EXPAND_MM, EXPAND_MM);
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
     *     <li>根节点宽高和 viewBox 使用“原尺寸 + 四边外扩量”。</li>
     *     <li>外层 g 带 img、data-source-name、data-forme、data-rotation 等算法/排版需要的元数据。</li>
     *     <li>第一个 path 是外扩后的大矩形轮廓。</li>
     *     <li>原 SVG 根节点内部内容会放入内层 g，并通过 translate(left, top) 移动到留白区域内。</li>
     * </ul>
     *
     * @param originalSvg 原始 mask SVG 文本
     * @param originalMaskUrl 原始 mask SVG 地址，用于写入 data-source-name
     * @param piece 当前生产工件，用于读取图片地址和生产工件 ID
     * @param originalWidth 原始 SVG 宽度
     * @param originalHeight 原始 SVG 高度
     * @param margins 四边外扩量
     * @return 新生成的外扩 SVG 文本
     */
    private String buildExpandedSvg(String originalSvg,
                                    String originalMaskUrl,
                                    ProductionPiece piece,
                                    double originalWidth,
                                    double originalHeight,
                                    ExpandMargins margins) {
        double newWidth = originalWidth + margins.left + margins.right;
        double newHeight = originalHeight + margins.top + margins.bottom;
        String inner = extractInnerSvg(originalSvg);
        String productImg = piece.getProductImageFile() == null ? "" : piece.getProductImageFile().getRawFile();
        String sourceName = sourceName(originalMaskUrl);
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + format(newWidth) + "\" height=\"" + format(newHeight)
                + "\" viewBox=\"0 0 " + format(newWidth) + " " + format(newHeight) + "\" version=\"1.1\" require-plt=\"true\">\n"
                + "<g id=\"liubai-" + escapeAttr(piece.getProductionPieceId()) + "\" img=\"" + escapeAttr(productImg)
                + "\" data-source-name=\"" + escapeAttr(sourceName) + "\" data-forme=\"false\" data-rotation=\"0\">\n"
                + "<path d=\"M0 0 H" + format(newWidth) + " V" + format(newHeight) + " H0 Z\" fill=\"#d1495b\" fill-opacity=\"0.82\" stroke=\"#111111\" stroke-width=\"1.23\" fill-rule=\"evenodd\" />\n"
                + "<g id=\"liubai-original\" transform=\"translate(" + format(margins.left) + " " + format(margins.top) + ")\">\n"
                + inner + "\n"
                + "</g>\n"
                + "</g></svg>";
    }

    /**
     * 提取原 SVG 根节点内部内容。
     *
     * <p>新 SVG 会重新生成根节点，因此需要去掉原 XML 声明和原 svg 根标签，只保留内部图形节点。</p>
     *
     * @param svg 原始 SVG 文本
     * @return 原 SVG 根节点内部内容
     */
    private String extractInnerSvg(String svg) {
        String withoutXml = svg.replaceFirst("(?is)^\\s*<\\?xml[^>]*>\\s*", "");
        Matcher matcher = SVG_INNER_PATTERN.matcher(withoutXml);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return withoutXml.trim();
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

package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DoubleSideMountingLayoutBuildService extends AbstractLayoutModeBuildService {
    private static final int TAG_DPI = 300;
    private static final double MM_PER_INCH = 25.4D;
    private static final int QR_LEFT_MM = 10;
    private static final int QR_SIZE_MM = 15;
    private static final int QR_BOTTOM_GAP_MM = 2;
    private static final int ELEMENT_GAP_MM = 30;
    private static final int EXTRA_INFO_GAP_MM = 5;
    private static final int NESTED_HEIGHT_EXPAND_THRESHOLD_MM = 2400;
    private static final int SIDE_EXPAND_MM = 6;
    private static final String TAG_TEXT_FONT = "Source Han Sans SC VF";
    private static final String LEFT_ARROW_URL = "https://craftstudio-mes-test.oss-cn-hangzhou.aliyuncs.com/basetag/leftarrow.png";
    private static final String RIGHT_ARROW_URL = "https://craftstudio-mes-test.oss-cn-hangzhou.aliyuncs.com/basetag/rightarrow.png";

    private final OssTagUploadService ossTagUploadService;

    public DoubleSideMountingLayoutBuildService(OssTagUploadService ossTagUploadService) {
        this.ossTagUploadService = ossTagUploadService;
    }
    /**
     * 圆形二维码排版模式构建器：
     * - 上下 margin 固定 30mm；
     * - marks 使用 C+B+A 拼接生成的标签条；
     * - 定位点使用 basetag/circle.svg。
     */
    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.DOUBLE_SIDE_MOUNTING_LAYOUT;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        // 1) 基于 mode 规则确定 margin 与元素原点（扩展矩形左上角为坐标原点）
        FormeLayoutBuildResult result = new FormeLayoutBuildResult();
        BigDecimal marginHeight = context.getMarginHeight();
        int nestedHeight = context.getNestedHeight().intValue();
        boolean needSideExpand = nestedHeight > NESTED_HEIGHT_EXPAND_THRESHOLD_MM;
        int marginLeft = needSideExpand ? SIDE_EXPAND_MM : 0;
        int marginTop = marginHeight.intValue();
        int marginRight = needSideExpand ? SIDE_EXPAND_MM : 0;
        int marginBottom = marginHeight.intValue();
        int elementOriginY = marginTop;
        int nestedStartX = marginLeft;

        FormeGenerationRequest.Margin margin = new FormeGenerationRequest.Margin();
        margin.setLeft(marginLeft);
        margin.setTop(marginTop);
        margin.setRight(marginRight);
        margin.setBottom(marginBottom);
        result.setMargin(margin);

        // 2) 构建 A/B/C/F：A=typesetting引用标识，B=队列plt名，C=二维码，F=标签条
        String elementA = context.getElementAResolver().apply(context.getTypesettingInfo());
        List<String> elementAExtInfos = buildElementAExtInfos(context.getTypesettingInfo());
        log.info("Circle QR tag text resolved, elementA={}, elementAExtInfos={}", elementA, elementAExtInfos);
        String elementB = context.getPlateNameSupplier().get();
        String elementBB = context.getPlateNameBBSupplier().get();
        String elementC = LEFT_ARROW_URL;
        String elementCC = LEFT_ARROW_URL;
        String manufacturerMetaId = context.getTypesettingInfo() == null ? null : context.getTypesettingInfo().getManufacturerMetaId();
        String typesettingId = context.getTypesettingInfo() == null ? null : context.getTypesettingInfo().getTypesettingId();
        String elementF = buildTagStripDataUri(context.getBusinessId(), manufacturerMetaId, typesettingId, elementA, elementAExtInfos, elementB, elementC, context.getNestedWidth(), marginHeight, false);
        String elementFRotated = buildTagStripDataUri(context.getBusinessId(), manufacturerMetaId, typesettingId, elementA, elementAExtInfos, elementBB, elementCC, context.getNestedWidth(), marginHeight, true);
        if (context.getTypesettingInfo() != null) {
            LinkedHashMap<String, String> marks = new LinkedHashMap<>();
            marks.put("elementF", elementF);
            marks.put("elementFRotated", elementFRotated);
            marks.put("elementG", LEFT_ARROW_URL);
            context.getTypesettingInfo().setMarks(marks);
        }

        // 3) 将标签条放置在上/下 margin 区域
        FormeGenerationRequest.Mark top = new FormeGenerationRequest.Mark();
        top.setImg(elementF);
        top.setSize(createSize(context.getNestedWidth(), marginHeight));
        top.setPosition(createPosition(nestedStartX, 0));

        FormeGenerationRequest.Mark bottom = new FormeGenerationRequest.Mark();
        bottom.setImg(elementFRotated);
        bottom.setSize(createSize(context.getNestedWidth(), marginHeight));
        bottom.setPosition(createPosition(nestedStartX, elementOriginY + context.getNestedHeight().intValue()));
        FormeGenerationRequest.Mark leftArrow = new FormeGenerationRequest.Mark();
        leftArrow.setImg(LEFT_ARROW_URL);
        leftArrow.setSize(createSize(BigDecimal.valueOf(QR_SIZE_MM), BigDecimal.valueOf(QR_SIZE_MM)));
        int arrowX = QR_LEFT_MM;
        int arrowY = (marginHeight.intValue() - QR_SIZE_MM) / 2;
        leftArrow.setPosition(createPosition(arrowX, arrowY));
        result.setMarks(Arrays.asList(top, bottom, leftArrow));

        // 双面对裱镜像印版不需要生成定位点
        result.setAnchorPoints(new ArrayList<>());

        // 5) 输出配置与上传目录
        result.setOutputs(buildDefaultOutputs(supportMode(), context, elementB, elementBB));
        result.setUploadPath("printingplate/");
        return result;
    }

    private List<String> buildElementAExtInfos(TypesettingInfo info) {
        List<String> extInfos = new ArrayList<>();
        if (info == null) {
            return extInfos;
        }
        if (StringUtils.isNotBlank(info.getTemplateCode()) && !"1/1".equals(info.getTemplateCode())) {
            extInfos.add(info.getTemplateCode());
        }
        extInfos.addAll(extractAccessoryLabels(info));
        return extInfos;
    }

    private List<String> extractAccessoryLabels(TypesettingInfo info) {
        if (info.getProcedureFlow() == null || info.getProcedureFlow().getNodes() == null) {
            return new ArrayList<>();
        }
        return info.getProcedureFlow().getNodes().stream()
                .filter(Objects::nonNull)
                .filter(node -> shouldExtractAccessoryLabel(node.getNodeName()))
                .map(this::extractAccessoryLabelFromNode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean shouldExtractAccessoryLabel(String nodeName) {
        if (StringUtils.isBlank(nodeName)) {
            return false;
        }
        return !"预处理".equals(nodeName)
                && !"待排版".equals(nodeName)
                && !"排版中".equals(nodeName)
                && !"打印中".equals(nodeName)
                && !"待打包".equals(nodeName)
                && !"已打包".equals(nodeName);
    }

    private String extractAccessoryLabelFromNode(ProcedureFlowNode node) {
        if (node.getParamConfigs() == null) {
            return "";
        }
        for (Object config : node.getParamConfigs()) {
            Object param = invokeGetter(config, "getParam");
            Object accessorySnapshot = param instanceof Map ? ((Map<?, ?>) param).get("accessorySnapshot") : invokeGetter(param, "getAccessorySnapshot");
            Object name = accessorySnapshot instanceof Map ? ((Map<?, ?>) accessorySnapshot).get("name") : invokeGetter(accessorySnapshot, "getName");
            if (name != null && StringUtils.isNotBlank(name.toString())) {
                return node.getNodeName() + "：<font color='red'>" + name + "</font>";
            }
        }
        return node.getNodeName() == null ? "" : node.getNodeName();
    }

    private Object invokeGetter(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception ignore) {
            return null;
        }
    }

    private String buildTagStripDataUri(String businessId,
                                        String manufacturerMetaId,
                                        String typesettingId,
                                        String elementA,
                                        List<String> elementAExtInfos,
                                        String elementB,
                                        String qrDataUri,
                                        BigDecimal stripWidth,
                                        BigDecimal stripHeight,
                                        boolean rotate180) {
        // 生成标签条 PNG 并上传至 OSS 的 /tag 路径，返回可访问 URL
        int stripHeightInt = stripHeight.intValue();
        int stripWidthInt = stripWidth.intValue();
        int canvasWidthPx = mmToPx(stripWidthInt);
        int canvasHeightPx = mmToPx(stripHeightInt);
        int qrLeftPx = mmToPx(QR_LEFT_MM);
        int qrSizePx = mmToPx(QR_SIZE_MM);
        int qrTopPx = canvasHeightPx - mmToPx(QR_BOTTOM_GAP_MM + QR_SIZE_MM);
        int bX = qrLeftPx + qrSizePx + mmToPx(ELEMENT_GAP_MM);
        int cX = bX + mmToPx(ELEMENT_GAP_MM);
        int textHeight = Math.max(mmToPx(4), 1);

        BufferedImage canvas = new BufferedImage(canvasWidthPx, canvasHeightPx, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, canvasWidthPx, canvasHeightPx);
            g.setColor(Color.BLACK);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(new Font(TAG_TEXT_FONT, Font.PLAIN, textHeight));
            FontMetrics fontMetrics = g.getFontMetrics();
            int textTopY = (canvasHeightPx - textHeight) / 2;
            int textBaseLineY = textTopY + ((textHeight - fontMetrics.getHeight()) / 2) + fontMetrics.getAscent();

            BufferedImage qrImage = decodePngDataUri(qrDataUri);
            if (qrImage != null) {
                BufferedImage effectiveQrImage = trimWhiteBorder(qrImage);
                g.drawImage(effectiveQrImage, qrLeftPx, qrTopPx, qrSizePx, qrSizePx, null);
            }
            drawTextRotate180(g, elementB, bX, textBaseLineY, fontMetrics);
            drawTextRotate180(g, elementA, cX, textBaseLineY, fontMetrics);
            int currentX = cX + fontMetrics.stringWidth(elementA == null ? "" : elementA) + mmToPx(EXTRA_INFO_GAP_MM);
            if (elementAExtInfos != null) {
                for (String extInfo : elementAExtInfos) {
                    if (StringUtils.isBlank(extInfo)) {
                        continue;
                    }
                    drawTextRotate180(g, extInfo, currentX, textBaseLineY, fontMetrics);
                    currentX += fontMetrics.stringWidth(extractDisplayText(extInfo)) + mmToPx(EXTRA_INFO_GAP_MM);
                }
            }

            BufferedImage uploadImage = rotate180 ? rotateCenter180(canvas) : canvas;

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(uploadImage, "png", outputStream);
            String uploadPath = buildMarkUploadPath(manufacturerMetaId, typesettingId);
            return ossTagUploadService.uploadTagPng(businessId, outputStream.toByteArray(), uploadPath);
        } catch (Exception e) {
            log.error("生成并上传标签条PNG失败:{}",e.getMessage(), e);
            e.printStackTrace();
            throw new IllegalStateException("生成并上传标签条PNG失败", e);
        } finally {
            g.dispose();
        }
    }

    private String buildMarkUploadPath(String manufacturerMetaId, String typesettingId) {
        if (StringUtils.isBlank(manufacturerMetaId) || StringUtils.isBlank(typesettingId)) {
            return "mark";
        }
        return "mark/" + manufacturerMetaId + "/" + typesettingId;
    }

    private void drawTextRotate180(Graphics2D g, String text, int x, int baselineY, FontMetrics fontMetrics) {
        String safeText = text == null ? "" : text;
        String plainText = extractDisplayText(safeText);
        int textWidth = fontMetrics.stringWidth(plainText);
        if (textWidth <= 0) {
            return;
        }
        int textHeight = fontMetrics.getHeight();
        double centerX = x + textWidth / 2.0D;
        double centerY = baselineY - fontMetrics.getAscent() + textHeight / 2.0D;

        AffineTransform origin = g.getTransform();
        Color originColor = g.getColor();
        try {
            g.rotate(Math.PI, centerX, centerY);
            drawRichText(g, safeText, x, baselineY, fontMetrics);
        } finally {
            g.setColor(originColor);
            g.setTransform(origin);
        }
    }

    private String extractDisplayText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("<font\\s+color='red'>", "").replace("</font>", "");
    }

    private void drawRichText(Graphics2D g, String text, int x, int baselineY, FontMetrics fontMetrics) {
        String safeText = text == null ? "" : text;
        String openTag = "<font color='red'>";
        String closeTag = "</font>";
        int openIdx = safeText.indexOf(openTag);
        int closeIdx = safeText.indexOf(closeTag);
        if (openIdx < 0 || closeIdx < 0 || closeIdx <= openIdx) {
            g.setColor(Color.BLACK);
            g.drawString(safeText, x, baselineY);
            return;
        }

        String prefix = safeText.substring(0, openIdx);
        String redText = safeText.substring(openIdx + openTag.length(), closeIdx);
        String suffix = safeText.substring(closeIdx + closeTag.length());

        int currentX = x;
        if (StringUtils.isNotEmpty(prefix)) {
            g.setColor(Color.BLACK);
            g.drawString(prefix, currentX, baselineY);
            currentX += fontMetrics.stringWidth(prefix);
        }
        if (StringUtils.isNotEmpty(redText)) {
            g.setColor(Color.RED);
            g.drawString(redText, currentX, baselineY);
            currentX += fontMetrics.stringWidth(redText);
        }
        if (StringUtils.isNotEmpty(suffix)) {
            g.setColor(Color.BLACK);
            g.drawString(suffix, currentX, baselineY);
        }
    }

    private Font pickFont(int textHeight, String elementA, String elementB, List<String> extInfos) {
        List<String> candidates = Arrays.asList("Microsoft YaHei", "SimHei", "Noto Sans CJK SC", "WenQuanYi Zen Hei", "Dialog");
        StringBuilder allText = new StringBuilder();
        if (elementA != null) allText.append(elementA);
        if (elementB != null) allText.append(elementB);
        if (extInfos != null) {
            for (String ext : extInfos) {
                if (ext != null) allText.append(ext);
            }
        }
        for (String name : candidates) {
            Font font = new Font(name, Font.PLAIN, textHeight);
            if (font.canDisplayUpTo(allText.toString()) == -1) {
                return font;
            }
        }
        return new Font("Dialog", Font.PLAIN, textHeight);
    }

    private BufferedImage decodePngDataUri(String dataUri) {
        if (dataUri == null || !dataUri.startsWith("data:image/png;base64,")) {
            return null;
        }
        try {
            String base64 = dataUri.substring("data:image/png;base64,".length());
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("解析二维码 PNG Data URI 失败", e);
        }
    }

    private BufferedImage trimWhiteBorder(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                int alpha = (rgb >>> 24) & 0xFF;
                int red = (rgb >>> 16) & 0xFF;
                int green = (rgb >>> 8) & 0xFF;
                int blue = rgb & 0xFF;
                boolean isWhitePixel = alpha == 0 || (red > 245 && green > 245 && blue > 245);
                if (!isWhitePixel) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return source;
        }
        return source.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private BufferedImage rotateCenter180(BufferedImage source) {
        AffineTransform transform = AffineTransform.getRotateInstance(Math.PI, source.getWidth() / 2.0D, source.getHeight() / 2.0D);
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        return op.filter(source, null);
    }

    private int mmToPx(int mm) {
        return Math.max((int) Math.round(mm * TAG_DPI / MM_PER_INCH), 1);
    }

}

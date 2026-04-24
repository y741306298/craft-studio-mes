package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import io.micrometer.common.util.StringUtils;
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
import java.util.Arrays;

@Service
public class SquareQrLayoutBuildService extends AbstractLayoutModeBuildService {
    private static final int TAG_DPI = 300;
    private static final double MM_PER_INCH = 25.4D;
    private static final int QR_LEFT_MM = 10;
    private static final int QR_SIZE_MM = 15;
    private static final int QR_BOTTOM_GAP_MM = 2;
    private static final int ELEMENT_GAP_MM = 30;
    private static final int ANCHOR_SIZE_MM = 4;
    private static final int ANCHOR_GAP_TO_MARGIN_BOTTOM_MM = 2;
    private static final int TOP_ANCHOR_LEFT_MM = QR_LEFT_MM + QR_SIZE_MM + 15;
    private static final int TOP_ANCHOR_RIGHT_MM = 80;
    private static final int BOTTOM_ANCHOR_LEFT_MM = 80;
    private static final int BOTTOM_ANCHOR_RIGHT_MM = 40;

    private final OssTagUploadService ossTagUploadService;

    public SquareQrLayoutBuildService(OssTagUploadService ossTagUploadService) {
        this.ossTagUploadService = ossTagUploadService;
    }

    /**
     * 方形二维码排版模式构建器：
     * - 上下 margin 固定 30mm；
     * - marks 使用 C+B+A 拼接生成的标签条；
     * - 定位点使用 basetag/square.svg。
     */
    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.SHAPED_CUTTING_PLT_QR_SQUARE;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        BigDecimal anchorSize = BigDecimal.valueOf(ANCHOR_SIZE_MM);
        // 1) 基于 mode 规则确定 margin 与元素原点（扩展矩形左上角为坐标原点）
        FormeLayoutBuildResult result = new FormeLayoutBuildResult();
        BigDecimal marginHeight = context.getMarginHeight();
        int marginLeft = 0;
        int marginTop = marginHeight.intValue();
        int marginRight = 0;
        int marginBottom = marginHeight.intValue();
        int elementOriginX = marginLeft;
        int elementOriginY = marginTop;

        FormeGenerationRequest.Margin margin = new FormeGenerationRequest.Margin();
        margin.setLeft(marginLeft);
        margin.setTop(marginTop);
        margin.setRight(marginRight);
        margin.setBottom(marginBottom);
        result.setMargin(margin);

        // 2) 构建 A/B/C/F：A=typesetting引用标识，B=队列plt名，C=二维码，F=标签条
        String elementA = context.getElementAResolver().apply(context.getTypesettingInfo());
        String elementB = context.getPlateNameSupplier().get();
        String elementBB = context.getPlateNameBBSupplier().get();
        String elementC = context.getQrDataUriGenerator().apply(elementB);
        String elementCC = context.getQrDataUriGenerator().apply(elementBB);
        String manufacturerMetaId = context.getTypesettingInfo() == null ? null : context.getTypesettingInfo().getManufacturerMetaId();
        String elementF = buildTagStripDataUri(context.getBusinessId(), manufacturerMetaId, elementA, elementB, elementC, context.getNestedWidth(), marginHeight, false);
        String elementFRotated = buildTagStripDataUri(context.getBusinessId(), manufacturerMetaId, elementA, elementBB, elementCC, context.getNestedWidth(), marginHeight, true);

        // 3) 将标签条放置在上/下 margin 区域
        FormeGenerationRequest.Mark top = new FormeGenerationRequest.Mark();
        top.setImg(elementF);
        top.setSize(createSize(context.getNestedWidth(), marginHeight));
        top.setPosition(createPosition(elementOriginX, 0));

        FormeGenerationRequest.Mark bottom = new FormeGenerationRequest.Mark();
        bottom.setImg(elementFRotated);
        bottom.setSize(createSize(context.getNestedWidth(), marginHeight));
        bottom.setPosition(createPosition(elementOriginX, elementOriginY + context.getNestedHeight().intValue()));
        result.setMarks(Arrays.asList(top, bottom));

        // 4) 在上/下 margin 区域插入 4 个方形定位点（左右各 30mm）
        int topY = marginTop - ANCHOR_GAP_TO_MARGIN_BOTTOM_MM - ANCHOR_SIZE_MM;
        int bottomY = elementOriginY + context.getNestedHeight().intValue() + ANCHOR_GAP_TO_MARGIN_BOTTOM_MM;
        int width = context.getNestedWidth().intValue();
        int topRightX = Math.max(elementOriginX + width - TOP_ANCHOR_RIGHT_MM - ANCHOR_SIZE_MM, elementOriginX + TOP_ANCHOR_LEFT_MM);
        int bottomLeftX = elementOriginX + BOTTOM_ANCHOR_LEFT_MM;
        int bottomRightX = Math.max(elementOriginX + width - BOTTOM_ANCHOR_RIGHT_MM - ANCHOR_SIZE_MM, bottomLeftX);
        String squareSvgUrl = "https://craftstudio-mes-test.oss-cn-hangzhou.aliyuncs.com/basetag/square.svg";

        FormeGenerationRequest.AnchorPoint tl = new FormeGenerationRequest.AnchorPoint();
        tl.setImg("square.png");
        tl.setSvg(squareSvgUrl);
        tl.setSize(createSize(anchorSize, anchorSize));
        tl.setPosition(createPosition(elementOriginX + TOP_ANCHOR_LEFT_MM, topY));

        FormeGenerationRequest.AnchorPoint tr = new FormeGenerationRequest.AnchorPoint();
        tr.setImg("square.png");
        tr.setSvg(squareSvgUrl);
        tr.setSize(createSize(anchorSize, anchorSize));
        tr.setPosition(createPosition(topRightX, topY));

        FormeGenerationRequest.AnchorPoint bl = new FormeGenerationRequest.AnchorPoint();
        bl.setImg("square.png");
        bl.setSvg(squareSvgUrl);
        bl.setSize(createSize(anchorSize, anchorSize));
        bl.setPosition(createPosition(bottomLeftX, bottomY));

        FormeGenerationRequest.AnchorPoint br = new FormeGenerationRequest.AnchorPoint();
        br.setImg("square.png");
        br.setSvg(squareSvgUrl);
        br.setSize(createSize(anchorSize, anchorSize));
        br.setPosition(createPosition(bottomRightX, bottomY));
        result.setAnchorPoints(Arrays.asList(tl, tr, bl, br));

        // 5) 输出配置与上传目录
        result.setOutputs(buildDefaultOutputs(supportMode(), context, elementB, elementBB));
        result.setUploadPath("printingplate/");
        return result;
    }

    private String buildTagStripDataUri(String businessId,
                                        String manufacturerMetaId,
                                        String elementA,
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
            g.setFont(new Font("SansSerif", Font.PLAIN, textHeight));
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

            BufferedImage uploadImage = rotate180 ? rotateCenter180(canvas) : canvas;

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(uploadImage, "png", outputStream);
            String uploadPath = StringUtils.isBlank(manufacturerMetaId) ? "qr" : "qr/" + manufacturerMetaId;
            return ossTagUploadService.uploadTagPng(businessId, outputStream.toByteArray(), uploadPath);
        } catch (Exception e) {
            throw new IllegalStateException("生成并上传标签条PNG失败", e);
        } finally {
            g.dispose();
        }
    }

    private void drawTextRotate180(Graphics2D g, String text, int x, int baselineY, FontMetrics fontMetrics) {
        String safeText = text == null ? "" : text;
        int textWidth = fontMetrics.stringWidth(safeText);
        if (textWidth <= 0) {
            return;
        }
        int textHeight = fontMetrics.getHeight();
        double centerX = x + textWidth / 2.0D;
        double centerY = baselineY - fontMetrics.getAscent() + textHeight / 2.0D;

        AffineTransform origin = g.getTransform();
        try {
            g.rotate(Math.PI, centerX, centerY);
            g.drawString(safeText, x, baselineY);
        } finally {
            g.setTransform(origin);
        }
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

package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingElement;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class CaifuLayoutBuildService extends AbstractLayoutModeBuildService {
    private static final int EXPAND_LEFT_MM = 10;
    private static final int EXPAND_RIGHT_MM = 13;

    private static final int MARK_B_WIDTH_MM = 8;
    private static final int MARK_B_HEIGHT_MM = 3;
    private static final int MARK_B_OFFSET_Y_MM = 255;

    private static final int MARK_C_WIDTH_MM = 3;
    private static final int MARK_C_OFFSET_X_MM = 20;
    protected static final int OPEN_BACK_TAG_HEIGHT_MM = 20;
    private static final int TAG_DPI = 300;
    private static final double MM_PER_INCH = 25.4D;
    private static final int TAG_TEXT_LEFT_MM = 30;
    private static final int TAG_TEXT_GAP_MM = 5;
    private static final String TAG_TEXT_FONT = "Source Han Sans SC VF";

    protected final OssTagUploadService ossTagUploadService;

    public CaifuLayoutBuildService(OssTagUploadService ossTagUploadService) {
        this.ossTagUploadService = ossTagUploadService;
    }

    /** XY切割-裁赋模式构建器。 */
    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.XY_CUTTING_AUX_LINE_CAIFU_A20PR0;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        int originalWidth = context.getNestedWidth().intValue();
        int originalHeight = context.getNestedHeight().intValue();
        int expandedHeight = originalHeight;

        FormeLayoutBuildResult result = new FormeLayoutBuildResult();
        FormeGenerationRequest.Margin margin = new FormeGenerationRequest.Margin();
        margin.setLeft(EXPAND_LEFT_MM);
        margin.setTop(0);
        margin.setRight(EXPAND_RIGHT_MM);
        margin.setBottom(0);
        result.setMargin(margin);
        String tagUploadSubDir = buildTagUploadSubDir(context);

        String elementB = ossTagUploadService.uploadTagPng(
                context.getBusinessId(),
                createBlackPng(MARK_B_WIDTH_MM, MARK_B_HEIGHT_MM),
                tagUploadSubDir
        );
        String elementC = ossTagUploadService.uploadTagPng(
                context.getBusinessId(),
                createBlackPng(MARK_C_WIDTH_MM, expandedHeight),
                tagUploadSubDir
        );

        List<FormeGenerationRequest.Mark> marks = new ArrayList<>();

        LinkedHashSet<Double> ys = new LinkedHashSet<>();
        ys.add(0D);
        TypesettingElement.GridLines gridLines = context.getTypesettingInfo() != null
                && context.getTypesettingInfo().getElement() != null
                ? context.getTypesettingInfo().getElement().getGridLines()
                : null;
        if (gridLines != null && gridLines.getYs() != null) {
            ys.addAll(gridLines.getYs());
        }

        int rightBMarkX = EXPAND_LEFT_MM + originalWidth;
        for (Double y : ys) {
            if (y == null) {
                continue;
            }
            if (y + MARK_B_OFFSET_Y_MM > expandedHeight) {
                continue;
            }
            int bMarkY = (int) Math.round(y + MARK_B_OFFSET_Y_MM);
            marks.add(createMark(elementB, MARK_B_WIDTH_MM, MARK_B_HEIGHT_MM, 0, bMarkY));
            marks.add(createMark(elementB, MARK_B_WIDTH_MM, MARK_B_HEIGHT_MM, rightBMarkX, bMarkY));
        }

        marks.add(createMark(
                elementC,
                MARK_C_WIDTH_MM,
                expandedHeight,
                MARK_C_OFFSET_X_MM + originalWidth,
                0
        ));

        if (context.getTypesettingInfo() != null) {
            LinkedHashMap<String, String> markFiles = new LinkedHashMap<>();
            markFiles.put("elementB", elementB);
            markFiles.put("elementC", elementC);
            context.getTypesettingInfo().setMarks(markFiles);
        }

        result.setMarks(marks);
        result.setAnchorPoints(Collections.emptyList());
        result.setOutputs(buildDefaultOutputs(supportMode(), context));
        result.setUploadPath("forme/" + context.getBusinessId() + "/");
        return result;
    }

    protected String buildTagUploadSubDir(FormeBuildContext context) {
        String manufacturerMetaId = context.getTypesettingInfo() == null ? null : context.getTypesettingInfo().getManufacturerMetaId();
        String typesettingId = context.getTypesettingInfo() == null ? null : context.getTypesettingInfo().getTypesettingId();
        if (isBlank(manufacturerMetaId) || isBlank(typesettingId)) {
            return "mark";
        }
        return "mark/" + manufacturerMetaId + "/" + typesettingId;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    protected FormeGenerationRequest.Mark createMark(String img, int width, int height, int x, int y) {
        FormeGenerationRequest.Mark mark = new FormeGenerationRequest.Mark();
        mark.setImg(img);
        mark.setSize(createSize(BigDecimal.valueOf(width), BigDecimal.valueOf(height)));
        mark.setPosition(createPosition(Math.max(0, x), Math.max(0, y)));
        return mark;
    }


    protected FormeGenerationRequest.Mark createMark(String img, int width, double height, int x, double y) {
        FormeGenerationRequest.Mark mark = new FormeGenerationRequest.Mark();
        mark.setImg(img);
        mark.setSize(createSize(BigDecimal.valueOf(width), BigDecimal.valueOf(height)));
        mark.setPosition(createPosition(Math.max(0, x), (int) Math.max(0, Math.round(y))));
        return mark;
    }

    protected String buildOpenBackTagStrip(FormeBuildContext context, int stripWidth, boolean rotate180) {
        String elementA = context.getElementAResolver() == null || context.getTypesettingInfo() == null
                ? ""
                : context.getElementAResolver().apply(context.getTypesettingInfo());
        int canvasWidthPx = mmToPx(stripWidth);
        int canvasHeightPx = mmToPx(OPEN_BACK_TAG_HEIGHT_MM);
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
            int baselineY = ((canvasHeightPx - textHeight) / 2) + ((textHeight - fontMetrics.getHeight()) / 2) + fontMetrics.getAscent();
            int currentX = mmToPx(TAG_TEXT_LEFT_MM);
            if (elementA != null && !elementA.trim().isEmpty()) {
                drawTextRotate180(g, elementA, currentX, baselineY, fontMetrics);
                currentX += fontMetrics.stringWidth(elementA) + mmToPx(TAG_TEXT_GAP_MM);
            }
            BufferedImage uploadImage = rotate180 ? rotateCenter180(canvas) : canvas;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(uploadImage, "png", outputStream);
            return ossTagUploadService.uploadTagPng(context.getBusinessId(), outputStream.toByteArray(), buildTagUploadSubDir(context));
        } catch (Exception e) {
            throw new IllegalStateException("生成开背标签条PNG失败", e);
        } finally {
            g.dispose();
        }
    }

    private void drawTextRotate180(Graphics2D g, String text, int x, int baselineY, FontMetrics fontMetrics) {
        int textWidth = fontMetrics.stringWidth(text);
        if (textWidth <= 0) {
            return;
        }
        int textHeight = fontMetrics.getHeight();
        double centerX = x + textWidth / 2.0D;
        double centerY = baselineY - fontMetrics.getAscent() + textHeight / 2.0D;
        AffineTransform origin = g.getTransform();
        try {
            g.rotate(Math.PI, centerX, centerY);
            g.drawString(text, x, baselineY);
        } finally {
            g.setTransform(origin);
        }
    }

    private BufferedImage rotateCenter180(BufferedImage source) {
        BufferedImage rotated = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics2D g = rotated.createGraphics();
        try {
            g.rotate(Math.PI, source.getWidth() / 2.0D, source.getHeight() / 2.0D);
            g.drawImage(source, 0, 0, null);
            return rotated;
        } finally {
            g.dispose();
        }
    }

    private int mmToPx(int mm) {
        return (int) Math.round(mm / MM_PER_INCH * TAG_DPI);
    }

    protected byte[] createBlackPng(int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, width, height);
            g.dispose();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("生成黑色 PNG 失败", e);
        }
    }
}

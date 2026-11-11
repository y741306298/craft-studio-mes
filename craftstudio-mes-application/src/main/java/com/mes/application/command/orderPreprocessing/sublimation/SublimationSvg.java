package com.mes.application.command.orderPreprocessing.sublimation;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/** 热升华矩形遮罩分组，沿用打扣/留白的 img、require-plt 元信息。 */
public final class SublimationSvg {
    public static final String POINT_IMG = "https://craftstudio-mes-prod.oss-cn-hangzhou.aliyuncs.com/basetag/basetag-point.png";

    private SublimationSvg() { }

    public static String open(double width, double height) {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + number(width) + "\" height=\"" + number(height)
                + "\" viewBox=\"0 0 " + number(width) + " " + number(height) + "\">";
    }

    public static String original(String id, String imageUrl, double width, double height) {
        return "<g id=\"sublimation-original-" + escape(id) + "\" img=\"" + escape(imageUrl) + "\" require-plt=\"true\">"
                + "<path d=\"M0 0 H" + number(width) + " V" + number(height) + " H0 Z\" fill=\"#000000\"/></g>";
    }

    /** 反面不带任何 mark，仅包含独立出血图片的完整矩形。 */
    public static String forBack(String id, String backUrl, double width, double height) {
        return open(width, height) + original(id, backUrl, width, height) + "</svg>";
    }

    public static String pointKey(SublimationGeometry.Point point) {
        return "sublimation-" + point.kind().name().toLowerCase(java.util.Locale.ROOT) + "-" + point.key();
    }

    public static String point(String id, SublimationGeometry.Point point) {
        return "<g id=\"" + escape(pointKey(point) + "-" + id) + "\" img=\"" + POINT_IMG
                + "\" data-source-name=\"basetag-point.png\" data-forme=\"false\" data-rotation=\"0\" require-plt=\"false\""
                + " transform=\"translate(" + number(point.x() - 4) + " " + number(point.y() - 4) + ")\">"
                + "<path d=\"M0 4 A4 4 0 1 0 8 4 A4 4 0 1 0 0 4 Z\" fill=\"#000000\"/></g>";
    }

    /** 最外圈2mm使用偶奇填充，分组边界与整张透明PNG一致，不增加画布尺寸。 */
    public static String border(String id, String imageUrl, double width, double height) {
        return "<g id=\"sublimation-border-" + escape(id) + "\" img=\"" + escape(imageUrl)
                + "\" require-plt=\"false\" data-forme=\"false\">"
                + "<path d=\"M0 0 H" + number(width) + " V" + number(height) + " H0 Z M2 2 H"
                + number(width - 2) + " V" + number(height - 2) + " H2 Z\" fill=\"#000000\" fill-rule=\"evenodd\"/></g>";
    }

    public static byte[] borderPng(double width, double height) {
        double scale = Math.min(36D / 25.4D, 4096D / Math.max(width, height));
        int pixelsW = Math.max(1, (int) Math.ceil(width * scale));
        int pixelsH = Math.max(1, (int) Math.ceil(height * scale));
        BufferedImage image = new BufferedImage(pixelsW, pixelsH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.scale(pixelsW / width, pixelsH / height);
        graphics.setColor(Color.BLACK);
        graphics.setStroke(new BasicStroke(2F));
        graphics.draw(new java.awt.geom.Rectangle2D.Double(1, 1, width - 2, height - 2));
        graphics.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("生成热升华2mm黑边图片失败", e);
        }
    }

    private static String number(double value) {
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

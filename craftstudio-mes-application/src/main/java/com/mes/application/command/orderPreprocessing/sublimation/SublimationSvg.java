package com.mes.application.command.orderPreprocessing.sublimation;


/** 热升华矩形遮罩分组，沿用打扣/留白的 img、require-plt 元信息。 */
public final class SublimationSvg {
    public static final String POINT_IMG = "https://craftstudio-mes-prod.oss-cn-hangzhou.aliyuncs.com/basetag/basetag-point.png";

    private SublimationSvg() { }

    public static String open(double width, double height) {
        return com.mes.application.command.graphics.RemoteGraphics.svg("SUB_OPEN", com.mes.application.command.graphics.RemoteGraphics.params("width", width, "height", height));
    }

    /** 整体 SVG 排版时，mirrorConfig 的 key 必须指向这个实际画面分组。 */
    public static String originalGroupId(String pieceId) {
        return "sublimation-original-" + pieceId;
    }

    public static String original(String id, String imageUrl, double width, double height) {
        return com.mes.application.command.graphics.RemoteGraphics.svg("SUB_ORIGINAL", com.mes.application.command.graphics.RemoteGraphics.params("id", id, "imageUrl", imageUrl, "width", width, "height", height));
    }

    /** 反面不带任何 mark，仅包含独立出血图片的完整矩形。 */
    public static String forBack(String id, String backUrl, double width, double height) {
        return com.mes.application.command.graphics.RemoteGraphics.svg("SUB_BACK", com.mes.application.command.graphics.RemoteGraphics.params("id", id, "backUrl", backUrl, "width", width, "height", height));
    }

    public static String pointKey(SublimationGeometry.Point point) {
        return "sublimation-" + point.kind().name().toLowerCase(java.util.Locale.ROOT) + "-" + point.key();
    }

    public static String point(String id, SublimationGeometry.Point point) {
        return com.mes.application.command.graphics.RemoteGraphics.svg("SUB_POINT", com.mes.application.command.graphics.RemoteGraphics.params("id", id, "point", point));
    }

    /** 最外圈2mm使用偶奇填充，分组边界与整张透明PNG一致，不增加画布尺寸。 */
    public static String border(String id, String imageUrl, double width, double height) {
        return com.mes.application.command.graphics.RemoteGraphics.svg("SUB_BORDER", com.mes.application.command.graphics.RemoteGraphics.params("id", id, "imageUrl", imageUrl, "width", width, "height", height));
    }

    public static com.mes.application.command.graphics.RemotePng borderPng(double width, double height) {
        return com.mes.application.command.graphics.RemoteGraphics.png("SUBLIMATION_BORDER", com.mes.application.command.graphics.RemoteGraphics.params("width", width, "height", height));
    }

    private static String number(double value) {
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

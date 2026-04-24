package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模式构建抽象基类。
 *
 * <p>封装各模式共享的低层拼装能力，避免重复代码：
 * createSize / createPosition / default outputs。
 */
public abstract class AbstractLayoutModeBuildService implements TypesettingLayoutModeBuildService {
    private static final Pattern MATRIX_PATTERN = Pattern.compile("matrix\\s*\\(([^)]*)\\)");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?\\d*\\.?\\d+");

    /** 2D 边界框。 */
    protected static class BoundingBox {
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;

        private void include(double x, double y) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        private void include(double x, double y, AffineTransform transform) {
            Point2D transformed = transform.transform(new Point2D.Double(x, y), null);
            include(transformed.getX(), transformed.getY());
        }

        private void includeRect(double x, double y, double width, double height, AffineTransform transform) {
            include(x, y, transform);
            include(x + width, y, transform);
            include(x, y + height, transform);
            include(x + width, y + height, transform);
        }

        private boolean isEmpty() {
            return Double.isInfinite(minX) || Double.isInfinite(minY) || Double.isInfinite(maxX) || Double.isInfinite(maxY);
        }

        protected BigDecimal getMinX() {
            return BigDecimal.valueOf(minX);
        }

        protected BigDecimal getMinY() {
            return BigDecimal.valueOf(minY);
        }

        protected BigDecimal getMaxX() {
            return BigDecimal.valueOf(maxX);
        }

        protected BigDecimal getMaxY() {
            return BigDecimal.valueOf(maxY);
        }
    }

    /** 左下角坐标（单位：mm）。 */
    protected static class LeftBottomCoordinate {
        private final BigDecimal x;
        private final BigDecimal y;

        protected LeftBottomCoordinate(BigDecimal x, BigDecimal y) {
            this.x = x;
            this.y = y;
        }

        public BigDecimal getX() {
            return x;
        }

        public BigDecimal getY() {
            return y;
        }
    }

    /** 构建尺寸对象。 */
    protected FormeGenerationRequest.Size createSize(BigDecimal width, BigDecimal height) {
        FormeGenerationRequest.Size size = new FormeGenerationRequest.Size();
        size.setWidth(width);
        size.setHeight(height);
        return size;
    }

    /** 构建坐标对象（坐标系原点：扩展矩形左上角）。 */
    protected FormeGenerationRequest.Position createPosition(int x, int y) {
        FormeGenerationRequest.Position position = new FormeGenerationRequest.Position();
        position.setX(x);
        position.setY(y);
        return position;
    }

    /**
     * 构建通用 outputs。
     *
     * <p>依据 mode 的 requireJson/requirePlt/requireSvg 决定输出项。
     */
    protected FormeGenerationRequest.Outputs buildDefaultOutputs(TypesettingLayoutMode mode, FormeBuildContext context) {
        return buildDefaultOutputs(mode, context, null, null);
    }

    /**
     * 构建通用 outputs（可显式指定 plt 名称，避免重复调用 Supplier 造成名称漂移）。
     */
    protected FormeGenerationRequest.Outputs buildDefaultOutputs(TypesettingLayoutMode mode,
                                                                 FormeBuildContext context,
                                                                 String pltNormalName,
                                                                 String pltReverseName) {
        String businessId = context.getBusinessId();
        FormeGenerationRequest.Outputs outputs = new FormeGenerationRequest.Outputs();
        if (mode.isRequireJsonFile()) {
            FormeGenerationRequest.OutputConfig json = new FormeGenerationRequest.OutputConfig();
            json.setObjectName(businessId + ".json");
            FormeGenerationRequest.EnvConfig env = new FormeGenerationRequest.EnvConfig();
            env.setBasePath("d://test//");
            FormeGenerationRequest.DtpConfig dtp = new FormeGenerationRequest.DtpConfig();
            dtp.setNewpage("false");
            dtp.setShowmode("4");
            dtp.setAutoSaveFile("");
            dtp.setTpfSavePath("d:\\test\\" + businessId + ".tpf");
            env.setDtp(dtp);
            json.setEnv(env);
            outputs.setJson(json);
        }
        if (mode.isRequirePltFile()) {
            FormeGenerationRequest.OutputConfig plt = new FormeGenerationRequest.OutputConfig();
            plt.setDirection("h");
            FormeGenerationRequest.PltObjectName pltObjectName = new FormeGenerationRequest.PltObjectName();
            String normal = pltNormalName;
            String reverse = pltReverseName;
            if (StringUtils.isBlank(normal) && context.getPlateNameSupplier() != null) {
                normal = context.getPlateNameSupplier().get();
            }
            if (StringUtils.isBlank(reverse) && context.getPlateNameBBSupplier() != null) {
                reverse = context.getPlateNameBBSupplier().get();
            }
            if (StringUtils.isBlank(normal) && context.getPlateNameSupplier() != null) {
                normal = context.getPlateNameSupplier().get();
            }
            if (StringUtils.isBlank(reverse) && context.getPlateNameSupplier() != null) {
                reverse = context.getPlateNameSupplier().get();
            }
            pltObjectName.setNormal(StringUtils.isNotBlank(normal) ? normal : businessId + "-1.plt");
            pltObjectName.setReverse(StringUtils.isNotBlank(reverse) ? reverse : businessId + "-2.plt");
            plt.setObjectName(pltObjectName);
            outputs.setPlt(plt);
        }
        if (mode.isRequireSvgFile()) {
            FormeGenerationRequest.OutputConfig svg = new FormeGenerationRequest.OutputConfig();
            svg.setObjectName(businessId + ".svg");
            outputs.setFormeSvg(svg);
        }
        return outputs;
    }

    /**
     * 从 SVG 中解析印版（data-forme=true）区域的左下角坐标（单位 mm）。
     *
     * <p>仅对 path/rect 提供边界计算；会累计父子节点 matrix(a b c d e f) 变换。
     */
    protected LeftBottomCoordinate parseFormeLeftBottomCoordinate(String svgContent) {
        BoundingBox box = parseFormeBoundingBox(svgContent);
        return new LeftBottomCoordinate(box.getMinX(), box.getMaxY());
    }

    /** 从 SVG 中解析印版（data-forme=true）区域的左上角坐标（单位 mm）。 */
    protected LeftBottomCoordinate parseFormeLeftTopCoordinate(String svgContent) {
        BoundingBox box = parseFormeBoundingBox(svgContent);
        return new LeftBottomCoordinate(box.getMinX(), box.getMinY());
    }

    /** 从 SVG 中解析印版（data-forme=true）边界框。 */
    protected BoundingBox parseFormeBoundingBox(String svgContent) {
        if (StringUtils.isBlank(svgContent)) {
            throw new IllegalArgumentException("SVG 内容不能为空");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(svgContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            Element formeElement = findFirstFormeElement(document.getDocumentElement());
            if (formeElement == null) {
                throw new IllegalArgumentException("SVG 中未找到 data-forme=true 的印版节点");
            }
            BoundingBox box = new BoundingBox();
            collectBounds(formeElement, new AffineTransform(), box);
            if (box.isEmpty()) {
                throw new IllegalArgumentException("无法从印版节点解析有效几何边界");
            }
            return box;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("解析 SVG 印版左下角坐标失败", e);
        }
    }

    private Element findFirstFormeElement(Element element) {
        if (element == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(element.getAttribute("data-forme"))) {
            return element;
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element found = findFirstFormeElement((Element) child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void collectBounds(Element element, AffineTransform parentTransform, BoundingBox box) {
        AffineTransform currentTransform = new AffineTransform(parentTransform);
        currentTransform.concatenate(parseMatrixTransform(element.getAttribute("transform")));

        String tag = element.getTagName();
        if ("rect".equalsIgnoreCase(tag)) {
            double x = parseDouble(element.getAttribute("x"), 0D);
            double y = parseDouble(element.getAttribute("y"), 0D);
            double width = parseDouble(element.getAttribute("width"), 0D);
            double height = parseDouble(element.getAttribute("height"), 0D);
            box.includeRect(x, y, width, height, currentTransform);
        } else if ("path".equalsIgnoreCase(tag)) {
            includePathBounds(element.getAttribute("d"), currentTransform, box);
        }

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                collectBounds((Element) child, currentTransform, box);
            }
        }
    }

    private void includePathBounds(String d, AffineTransform transform, BoundingBox box) {
        if (StringUtils.isBlank(d)) {
            return;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(d);
        boolean isX = true;
        Double x = null;
        while (matcher.find()) {
            double value = Double.parseDouble(matcher.group());
            if (isX) {
                x = value;
            } else if (x != null) {
                box.include(x, value, transform);
            }
            isX = !isX;
        }
    }

    private AffineTransform parseMatrixTransform(String transformText) {
        if (StringUtils.isBlank(transformText)) {
            return new AffineTransform();
        }
        Matcher matrixMatcher = MATRIX_PATTERN.matcher(transformText);
        if (!matrixMatcher.find()) {
            return new AffineTransform();
        }
        Matcher numberMatcher = NUMBER_PATTERN.matcher(matrixMatcher.group(1));
        double[] values = new double[6];
        int idx = 0;
        while (numberMatcher.find() && idx < 6) {
            values[idx++] = Double.parseDouble(numberMatcher.group());
        }
        if (idx < 6) {
            return new AffineTransform();
        }
        return new AffineTransform(values[0], values[1], values[2], values[3], values[4], values[5]);
    }

    private double parseDouble(String value, double defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }
}

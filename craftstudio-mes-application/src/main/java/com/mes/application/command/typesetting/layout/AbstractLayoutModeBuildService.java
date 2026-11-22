package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.apache.commons.lang3.StringUtils;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模式构建抽象基类。
 *
 * <p>封装各模式共享的低层拼装能力，避免重复代码：
 * createSize / createPosition / default outputs。
 */
public abstract class AbstractLayoutModeBuildService implements TypesettingLayoutModeBuildService {
    private static final String LARGE_ANCHOR_MANUFACTURER_META_ID = "69f95b080ff1ad90a9611468";
    private static final int LARGE_ANCHOR_SIZE_MM = 6;
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

    /** 构建尺寸对象。 */
    protected FormeGenerationRequest.Size createSize(BigDecimal width, BigDecimal height) {
        FormeGenerationRequest.Size size = new FormeGenerationRequest.Size();
        size.setWidth(width);
        size.setHeight(height);
        return size;
    }

    /** 特定工厂统一使用 6mm × 6mm 定位点，其余工厂沿用排版模式的默认尺寸。 */
    protected int resolveAnchorSizeMm(FormeBuildContext context, int defaultSizeMm) {
        if (context != null
                && context.getTypesettingInfo() != null
                && LARGE_ANCHOR_MANUFACTURER_META_ID.equals(context.getTypesettingInfo().getManufacturerMetaId())) {
            return LARGE_ANCHOR_SIZE_MM;
        }
        return defaultSizeMm;
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
        String outputObjectBaseName = context.getTypesettingInfo() != null
                && StringUtils.isNotBlank(context.getTypesettingInfo().getId())
                ? context.getTypesettingInfo().getId()
                : businessId;
        FormeGenerationRequest.Outputs outputs = new FormeGenerationRequest.Outputs();
        if (mode.isRequireJsonFile()) {
            FormeGenerationRequest.OutputConfig json = new FormeGenerationRequest.OutputConfig();
            json.setObjectName(outputObjectBaseName + ".json");
            FormeGenerationRequest.EnvConfig env = new FormeGenerationRequest.EnvConfig();
            env.setBasePath("..\\images\\");
            FormeGenerationRequest.DtpConfig dtp = new FormeGenerationRequest.DtpConfig();
            dtp.setNewpage("false");
            dtp.setShowmode("4");
            dtp.setAutoSaveFile("");
            dtp.setTpfSavePath("..\\images\\" + outputObjectBaseName + ".tpf");
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
            svg.setObjectName(outputObjectBaseName + ".svg");
            outputs.setFormeSvg(svg);
        }
        return outputs;
    }

    /** 从首个 data-forme=true 节点解析边界，累计该节点及其子节点的 matrix 变换。 */
    protected BoundingBox parseFormeBoundingBox(XMLStreamReader xml) throws XMLStreamException {
        BoundingBox box = new BoundingBox();
        Deque<AffineTransform> transforms = new ArrayDeque<>();
        while (xml.hasNext()) {
            int event = xml.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (transforms.isEmpty() && !"true".equalsIgnoreCase(xml.getAttributeValue(null, "data-forme"))) {
                    continue;
                }
                AffineTransform parent = transforms.peek();
                AffineTransform current = parent == null ? new AffineTransform() : new AffineTransform(parent);
                current.concatenate(parseMatrixTransform(xml.getAttributeValue(null, "transform")));
                transforms.push(current);
                includeGeometry(xml, current, box);
            } else if (event == XMLStreamConstants.END_ELEMENT && !transforms.isEmpty()) {
                transforms.pop();
                if (transforms.isEmpty()) {
                    if (box.isEmpty()) {
                        throw new IllegalArgumentException("无法从印版节点解析有效几何边界");
                    }
                    return box;
                }
            }
        }
        throw new IllegalArgumentException("SVG 中未找到 data-forme=true 的印版节点");
    }

    private void includeGeometry(XMLStreamReader xml, AffineTransform transform, BoundingBox box) {
        String tag = xml.getLocalName();
        if ("rect".equalsIgnoreCase(tag)) {
            double x = parseDouble(xml.getAttributeValue(null, "x"), 0D);
            double y = parseDouble(xml.getAttributeValue(null, "y"), 0D);
            double width = parseDouble(xml.getAttributeValue(null, "width"), 0D);
            double height = parseDouble(xml.getAttributeValue(null, "height"), 0D);
            box.includeRect(x, y, width, height, transform);
        } else if ("path".equalsIgnoreCase(tag)) {
            includePathBounds(xml.getAttributeValue(null, "d"), transform, box);
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

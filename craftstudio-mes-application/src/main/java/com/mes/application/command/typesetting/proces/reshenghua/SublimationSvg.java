package com.mes.application.command.typesetting.proces.reshenghua;

import org.apache.batik.parser.AWTPathProducer;
import org.apache.batik.parser.AWTPolygonProducer;
import org.apache.batik.parser.AWTTransformProducer;
import org.apache.batik.parser.PathParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 热升华几何重写。统一映射到毫米坐标后计算轮廓，保留原内容和已有分组。
 * 外扩条带从对应方向的几何极值起算，跨度覆盖该方向原轮廓的包围范围。
 */
final class SublimationSvg {
    private static final String SVG_NS = "http://www.w3.org/2000/svg";
    private static final Pattern LENGTH = Pattern.compile("^([-+]?(?:\\d*\\.)?\\d+(?:[eE][-+]?\\d+)?)\\s*(mm|cm|px|in|pt)?$", Pattern.CASE_INSENSITIVE);
    private final Document document;
    private final Element root;
    private final Area original;
    private final AffineTransform toMillimeters;

    SublimationSvg(String svg, Double fallbackWidth, Double fallbackHeight) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(svg)));
            root = document.getDocumentElement();
            if (!"svg".equals(root.getLocalName())) {
                throw new IllegalArgumentException("热升华蒙版必须为 SVG");
            }
            toMillimeters = coordinateTransform(root, fallbackWidth, fallbackHeight);
            original = collect(root, toMillimeters, new HashSet<>());
            if (original.isEmpty()) {
                throw new IllegalArgumentException("热升华 SVG 没有可外扩的闭合轮廓");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("热升华 SVG 解析失败", e);
        }
    }

    Rectangle2D bounds() {
        return original.getBounds2D();
    }

    Area extension(Map<SewingDirection.Edge, Double> margins) {
        Rectangle2D b = bounds();
        double left = margins.getOrDefault(SewingDirection.Edge.LEFT, 0D);
        double right = margins.getOrDefault(SewingDirection.Edge.RIGHT, 0D);
        double top = margins.getOrDefault(SewingDirection.Edge.TOP, 0D);
        double bottom = margins.getOrDefault(SewingDirection.Edge.BOTTOM, 0D);
        Area strips = new Area();
        if (top > 0D) strips.add(new Area(new Rectangle2D.Double(b.getMinX() - left, b.getMinY() - top, b.getWidth() + left + right, top)));
        if (bottom > 0D) strips.add(new Area(new Rectangle2D.Double(b.getMinX() - left, b.getMaxY(), b.getWidth() + left + right, bottom)));
        if (left > 0D) strips.add(new Area(new Rectangle2D.Double(b.getMinX() - left, b.getMinY(), left, b.getHeight())));
        if (right > 0D) strips.add(new Area(new Rectangle2D.Double(b.getMaxX(), b.getMinY(), right, b.getHeight())));
        return strips;
    }

    Area expanded(Area extension) {
        Area shape = new Area(original);
        shape.add(extension);
        return shape;
    }

    /** 每个闭合子轮廓填成 Area 后并集，消除孔洞，只留下真正的外轮廓。 */
    static Area exterior(Area shape) {
        Area result = new Area();
        Path2D.Double contour = null;
        double[] c = new double[6];
        for (PathIterator it = shape.getPathIterator(null); !it.isDone(); it.next()) {
            int type = it.currentSegment(c);
            if (type == PathIterator.SEG_MOVETO) {
                contour = new Path2D.Double();
                contour.moveTo(c[0], c[1]);
            } else if (contour != null) {
                appendSegment(contour, type, c);
                if (type == PathIterator.SEG_CLOSE) {
                    result.add(new Area(contour));
                }
            }
        }
        return result;
    }

    String rewrite(String pieceId, String originalImg, Area strips, Area outline, String markUrl, String tagGroups) {
        // 原内容统一包裹一次物理单位变换，图形自身的 transform 和分组属性保持不变。
        Element content = element("g");
        AffineTransform contentTransform = new AffineTransform(toMillimeters);
        if (root.hasAttribute("transform")) contentTransform.concatenate(AWTTransformProducer.createAffineTransform(root.getAttribute("transform")));
        content.setAttribute("transform", matrix(contentTransform));
        boolean hasGroup = root.getElementsByTagNameNS("*", "g").getLength() > 0;
        if (!hasGroup) {
            content.setAttribute("id", pieceId);
            content.setAttribute("img", originalImg);
            content.setAttribute("data-rotation", "0");
        }
        while (root.hasChildNodes()) content.appendChild(root.getFirstChild());
        root.appendChild(content);
        root.removeAttribute("transform");
        Element extension = element("g");
        extension.setAttribute("id", "reshenghua-extension-" + pieceId);
        extension.setAttribute("data-forme", "false");
        extension.setAttribute("require-plt", "false");
        extension.appendChild(path(strips, "#d1495b"));
        root.insertBefore(extension, content);
        appendFragment(tagGroups);

        Element border = element("g");
        border.setAttribute("id", "reshenghua-outline-" + pieceId);
        border.setAttribute("img", markUrl);
        border.setAttribute("data-forme", "false");
        border.setAttribute("data-rotation", "0");
        border.setAttribute("require-plt", "false");
        Element stroke = path(outline, "none");
        stroke.setAttribute("stroke", "#111111");
        stroke.setAttribute("stroke-width", "2"); // 根 viewBox 已归一到 mm，2 个用户单位即 2mm。
        stroke.setAttribute("stroke-linejoin", "round");
        border.appendChild(stroke);
        root.appendChild(border); // SVG 后绘制的分组位于最上层。

        Rectangle2D b = outline.getBounds2D();
        root.setAttribute("width", number(b.getWidth()) + "mm");
        root.setAttribute("height", number(b.getHeight()) + "mm");
        root.setAttribute("viewBox", number(b.getX()) + " " + number(b.getY()) + " " + number(b.getWidth()) + " " + number(b.getHeight()));
        root.setAttribute("overflow", "visible"); // 居中描边向外的 1mm 不裁切，也不计入工艺外扩量。
        return serialize();
    }

    private Area collect(Element node, AffineTransform parent, Set<Element> references) throws java.io.IOException {
        String tag = node.getLocalName();
        if ("defs".equals(tag) || "clipPath".equals(tag) || "mask".equals(tag)
                || "metadata".equals(tag) || "title".equals(tag) || "desc".equals(tag)
                || "style".equals(tag) || "text".equals(tag) || "image".equals(tag)
                || isMark(node) || "none".equals(node.getAttribute("display"))) {
            return new Area();
        }
        if (node.hasAttribute("clip-path") || node.hasAttribute("mask")) {
            throw new IllegalArgumentException("热升华轮廓暂不支持 clip-path/mask，请先转换为闭合 path");
        }
        AffineTransform transform = new AffineTransform(parent);
        if (node.hasAttribute("transform")) transform.concatenate(AWTTransformProducer.createAffineTransform(node.getAttribute("transform")));
        if ("use".equals(tag)) {
            String ref = node.getAttribute("href");
            if (ref.isEmpty()) ref = node.getAttributeNS("http://www.w3.org/1999/xlink", "href");
            Element target = findById(root, ref.startsWith("#") ? ref.substring(1) : "");
            if (target == null || !references.add(target)) throw new IllegalArgumentException("热升华 SVG use 引用无效或循环");
            transform.translate(value(node, "x", 0), value(node, "y", 0));
            Area used = collect(target, transform, references);
            references.remove(target);
            return used;
        }
        Shape shape = switch (tag) {
            case "path" -> parsePath(node.getAttribute("d"),
                    "evenodd".equals(inherited(node, "fill-rule")) ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO);
            case "polygon" -> AWTPolygonProducer.createShape(new StringReader(node.getAttribute("points")), Path2D.WIND_NON_ZERO);
            case "rect" -> rectangle(node);
            case "circle" -> ellipse(node, value(node, "r", 0), value(node, "r", 0));
            case "ellipse" -> ellipse(node, value(node, "rx", 0), value(node, "ry", 0));
            default -> null;
        };
        Area area = shape == null ? new Area() : new Area(transform.createTransformedShape(shape));
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element e) area.add(collect(e, transform, references));
        }
        return area;
    }

    private static boolean isMark(Element e) {
        String id = e.getAttribute("id");
        return "false".equals(e.getAttribute("data-forme"))
                && !(id.startsWith("liubai-") && !id.startsWith("liubai-tag-"));
    }

    private static Shape parsePath(String data, int windingRule) throws java.io.IOException {
        PathParser parser = new PathParser();
        AWTPathProducer producer = new SvgPathProducer();
        producer.setWindingRule(windingRule);
        parser.setPathHandler(producer);
        parser.parse(new StringReader(data));
        return producer.getShape();
    }

    /**
     * Batik 默认 arcTo 在直径端点附近会人为放大半径，导致半圆极值偏移。
     * 按 SVG 端点到圆心公式转换，只有半径不足时才修正半径；其余命令仍复用 Batik。
     */
    private static class SvgPathProducer extends AWTPathProducer {
        @Override
        public void arcRel(float rx, float ry, float angle, boolean large, boolean sweep, float x, float y) {
            arcAbs(rx, ry, angle, large, sweep, currentX + x, currentY + y);
        }

        @Override
        public void arcAbs(float rxValue, float ryValue, float angle, boolean large, boolean sweep, float x, float y) {
            double rx = Math.abs((double) rxValue), ry = Math.abs((double) ryValue);
            if (rx == 0D || ry == 0D) {
                linetoAbs(x, y);
                return;
            }
            if (currentX == x && currentY == y) return;
            double phi = Math.toRadians(angle % 360D), cos = Math.cos(phi), sin = Math.sin(phi);
            double dx = (currentX - x) / 2D, dy = (currentY - y) / 2D;
            double px = cos * dx + sin * dy, py = -sin * dx + cos * dy;
            double ratio = px * px / (rx * rx) + py * py / (ry * ry);
            if (ratio > 1D) {
                rx *= Math.sqrt(ratio);
                ry *= Math.sqrt(ratio);
            }
            double rx2 = rx * rx, ry2 = ry * ry;
            double denominator = rx2 * py * py + ry2 * px * px;
            double factor = (large == sweep ? -1D : 1D) * Math.sqrt(Math.max(0D, (rx2 * ry2 - denominator) / denominator));
            double cxp = factor * rx * py / ry, cyp = -factor * ry * px / rx;
            double cx = cos * cxp - sin * cyp + (currentX + x) / 2D;
            double cy = sin * cxp + cos * cyp + (currentY + y) / 2D;
            double ux = (px - cxp) / rx, uy = (py - cyp) / ry;
            double vx = (-px - cxp) / rx, vy = (-py - cyp) / ry;
            double start = Math.atan2(uy, ux);
            double extent = Math.atan2(ux * vy - uy * vx, ux * vx + uy * vy);
            if (sweep && extent < 0D) extent += Math.PI * 2D;
            if (!sweep && extent > 0D) extent -= Math.PI * 2D;
            Arc2D arc = new Arc2D.Double(cx - rx, cy - ry, rx * 2D, ry * 2D,
                    -Math.toDegrees(start), -Math.toDegrees(extent), Arc2D.OPEN);
            path.append(AffineTransform.getRotateInstance(phi, cx, cy).createTransformedShape(arc), true);
            xCenter = currentX = x;
            yCenter = currentY = y;
        }
    }

    private static String inherited(Element e, String attribute) {
        if (e.hasAttribute(attribute)) return e.getAttribute(attribute);
        return e.getParentNode() instanceof Element parent ? inherited(parent, attribute) : "";
    }

    private static Element findById(Element e, String id) {
        if (id.isEmpty()) return null;
        if (id.equals(e.getAttribute("id"))) return e;
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element child) {
                Element found = findById(child, id);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Shape rectangle(Element e) {
        double x = value(e, "x", 0), y = value(e, "y", 0);
        double w = value(e, "width", 0), h = value(e, "height", 0);
        double rx = value(e, "rx", value(e, "ry", 0)), ry = value(e, "ry", rx);
        return rx > 0 || ry > 0 ? new RoundRectangle2D.Double(x, y, w, h, Math.min(w, 2 * rx), Math.min(h, 2 * ry))
                : new Rectangle2D.Double(x, y, w, h);
    }

    private static Shape ellipse(Element e, double rx, double ry) {
        return new Ellipse2D.Double(value(e, "cx", 0) - rx, value(e, "cy", 0) - ry, 2 * rx, 2 * ry);
    }

    private static double value(Element e, String name, double fallback) {
        if (!e.hasAttribute(name)) return fallback;
        // 几何属性沿用 path 的用户坐标；物理单位只在根节点转换，避免混用单位时静默算错。
        String text = e.getAttribute(name);
        try {
            double result = Double.parseDouble(text);
            if (Double.isFinite(result)) return result;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("SVG 几何属性必须是用户坐标数值: " + name + "=" + text);
    }

    private static AffineTransform coordinateTransform(Element root, Double fallbackWidth, Double fallbackHeight) {
        double width = physicalLength(root.getAttribute("width"), fallbackWidth);
        double height = physicalLength(root.getAttribute("height"), fallbackHeight);
        String viewBox = root.getAttribute("viewBox").trim();
        double x = 0D, y = 0D;
        double userWidth = numericLength(root.getAttribute("width"), width);
        double userHeight = numericLength(root.getAttribute("height"), height);
        if (!viewBox.isEmpty()) {
            String[] parts = viewBox.split("[\\s,]+");
            if (parts.length != 4) throw new IllegalArgumentException("SVG viewBox 格式错误");
            x = Double.parseDouble(parts[0]); y = Double.parseDouble(parts[1]);
            userWidth = Double.parseDouble(parts[2]); userHeight = Double.parseDouble(parts[3]);
        }
        if (!(width > 0 && height > 0 && userWidth > 0 && userHeight > 0)
                || !Double.isFinite(width + height + userWidth + userHeight + x + y)) {
            throw new IllegalArgumentException("SVG 宽高/viewBox 必须为有效正尺寸");
        }
        double sx = width / userWidth, sy = height / userHeight, dx = 0D, dy = 0D;
        String aspect = root.getAttribute("preserveAspectRatio").trim();
        if (!viewBox.isEmpty() && !"none".equals(aspect)) {
            double scale = aspect.contains("slice") ? Math.max(sx, sy) : Math.min(sx, sy);
            dx = aspect.contains("xMin") ? 0D : (width - userWidth * scale) * (aspect.contains("xMax") ? 1D : .5D);
            dy = aspect.contains("YMin") ? 0D : (height - userHeight * scale) * (aspect.contains("YMax") ? 1D : .5D);
            sx = scale; sy = scale;
        }
        AffineTransform transform = AffineTransform.getTranslateInstance(dx, dy);
        transform.scale(sx, sy);
        transform.translate(-x, -y);
        return transform;
    }

    private static double numericLength(String text, double fallback) {
        Matcher matcher = LENGTH.matcher(text.trim());
        return matcher.matches() ? Double.parseDouble(matcher.group(1)) : fallback;
    }

    private static double physicalLength(String text, Double fallback) {
        if (text.isBlank()) return fallback == null ? 0D : fallback;
        Matcher matcher = LENGTH.matcher(text.trim());
        if (!matcher.matches()) throw new IllegalArgumentException("不支持的 SVG 尺寸: " + text);
        double value = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2);
        // MES 历史 SVG 无单位时以 mm 约定；显式 px 按 SVG 标准 96dpi 换算。
        return value * (unit == null ? 1D : switch (unit.toLowerCase(Locale.ROOT)) {
            case "cm" -> 10D;
            case "px" -> 25.4D / 96D;
            case "in" -> 25.4D;
            case "pt" -> 25.4D / 72D;
            default -> 1D;
        });
    }

    private Element element(String name) { return document.createElementNS(SVG_NS, name); }

    private Element path(Shape shape, String fill) {
        Element path = element("path");
        path.setAttribute("d", pathData(shape));
        path.setAttribute("fill", fill);
        path.setAttribute("fill-rule", "evenodd");
        path.setAttribute("require-plt", "false");
        return path;
    }

    static String pathData(Shape shape) {
        StringBuilder data = new StringBuilder();
        double[] c = new double[6];
        for (PathIterator it = shape.getPathIterator(null); !it.isDone(); it.next()) {
            int type = it.currentSegment(c);
            data.append("MLQCZ".charAt(type));
            int count = switch (type) { case 0, 1 -> 2; case 2 -> 4; case 3 -> 6; default -> 0; };
            for (int i = 0; i < count; i++) data.append(number(c[i])).append(' ');
        }
        return data.toString();
    }

    private static void appendSegment(Path2D.Double path, int type, double[] c) {
        switch (type) {
            case PathIterator.SEG_LINETO -> path.lineTo(c[0], c[1]);
            case PathIterator.SEG_QUADTO -> path.quadTo(c[0], c[1], c[2], c[3]);
            case PathIterator.SEG_CUBICTO -> path.curveTo(c[0], c[1], c[2], c[3], c[4], c[5]);
            case PathIterator.SEG_CLOSE -> path.closePath();
            default -> throw new IllegalArgumentException("无效轮廓命令");
        }
    }

    private static String matrix(AffineTransform transform) {
        double[] m = new double[6];
        transform.getMatrix(m);
        return "matrix(" + number(m[0]) + " " + number(m[1]) + " " + number(m[2]) + " " + number(m[3]) + " " + number(m[4]) + " " + number(m[5]) + ")";
    }

    private void appendFragment(String fragment) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document tags = factory.newDocumentBuilder().parse(new InputSource(new StringReader("<svg xmlns=\"" + SVG_NS + "\">" + fragment + "</svg>")));
            for (Node n = tags.getDocumentElement().getFirstChild(); n != null; n = n.getNextSibling()) root.appendChild(document.importNode(n, true));
        } catch (Exception e) {
            throw new IllegalArgumentException("热升华标签 SVG 构建失败", e);
        }
    }

    private String serialize() {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            var transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("热升华 SVG 序列化失败", e);
        }
    }

    static String number(double value) {
        return String.format(Locale.ROOT, "%.8f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}

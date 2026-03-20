package com.mes.interfaces.api.test;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 将 SVG 中基础图形转换为 HPGL(PLT) 指令。
 * 输出格式示例：IN;PA;LT;SP2;PU100,100;PD200,200;LT;SP0;PU;PG;
 */
public final class SvgToPltConverter {

    private static final DecimalFormat NUMBER_FORMAT =
            new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.US));

    static {
        NUMBER_FORMAT.setMaximumFractionDigits(0);
    }

    private SvgToPltConverter() {
    }

    public static void convertSvgFileToPlt(Path svgPath, Path pltPath) throws Exception {
        String svgContent = Files.readString(svgPath);
        String pltContent = convertSvgContentToPlt(svgContent);
        if (pltPath.getParent() != null) {
            Files.createDirectories(pltPath.getParent());
        }
        Files.writeString(pltPath, pltContent);
    }

    public static String convertSvgContentToPlt(String svgContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        Document document = factory.newDocumentBuilder().parse(new InputSource(new java.io.StringReader(svgContent)));

        PltBuilder plt = new PltBuilder();
        plt.append("IN").append("PA").append("LT").append("SP2");

        Element root = document.getDocumentElement();
        traverse(root, plt);

        plt.append("LT").append("SP0").append("PU").append("PG");
        return plt.build();
    }

    private static void traverse(Element element, PltBuilder plt) {
        switch (element.getTagName()) {
            case "line" -> appendLine(element, plt);
            case "polyline" -> appendPoly(element, plt, false);
            case "polygon" -> appendPoly(element, plt, true);
            case "rect" -> appendRect(element, plt);
            case "circle" -> appendCircle(element, plt);
            case "path" -> appendPath(element, plt);
            default -> {
            }
        }

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element child) {
                traverse(child, plt);
            }
        }
    }

    private static void appendLine(Element element, PltBuilder plt) {
        double x1 = getDouble(element, "x1", 0);
        double y1 = getDouble(element, "y1", 0);
        double x2 = getDouble(element, "x2", 0);
        double y2 = getDouble(element, "y2", 0);
        plt.pu(x1, y1).pd(x2, y2);
    }

    private static void appendPoly(Element element, PltBuilder plt, boolean closePath) {
        List<Point> points = parsePoints(element.getAttribute("points"));
        if (points.isEmpty()) {
            return;
        }
        Point first = points.getFirst();
        plt.pu(first.x, first.y);
        for (int i = 1; i < points.size(); i++) {
            Point p = points.get(i);
            plt.pd(p.x, p.y);
        }
        if (closePath && points.size() > 2) {
            plt.pd(first.x, first.y);
        }
    }

    private static void appendRect(Element element, PltBuilder plt) {
        double x = getDouble(element, "x", 0);
        double y = getDouble(element, "y", 0);
        double width = getDouble(element, "width", 0);
        double height = getDouble(element, "height", 0);
        if (width <= 0 || height <= 0) {
            return;
        }
        plt.pu(x, y).pd(x + width, y).pd(x + width, y + height).pd(x, y + height).pd(x, y);
    }

    private static void appendCircle(Element element, PltBuilder plt) {
        double cx = getDouble(element, "cx", 0);
        double cy = getDouble(element, "cy", 0);
        double r = getDouble(element, "r", 0);
        if (r <= 0) {
            return;
        }
        plt.pu(cx, cy).ci(r);
    }

    private static void appendPath(Element element, PltBuilder plt) {
        String d = element.getAttribute("d");
        if (d == null || d.isBlank()) {
            return;
        }

        List<String> tokens = tokenizePath(d);
        double currX = 0;
        double currY = 0;
        double startX = 0;
        double startY = 0;
        char command = 0;

        for (int i = 0; i < tokens.size(); ) {
            String token = tokens.get(i);
            if (isCommand(token)) {
                command = token.charAt(0);
                i++;
            }

            switch (command) {
                case 'M', 'm' -> {
                    if (i + 1 >= tokens.size()) {
                        return;
                    }
                    double x = parseDouble(tokens.get(i));
                    double y = parseDouble(tokens.get(i + 1));
                    i += 2;
                    if (command == 'm') {
                        x += currX;
                        y += currY;
                    }
                    currX = x;
                    currY = y;
                    startX = x;
                    startY = y;
                    plt.pu(currX, currY);
                    command = command == 'M' ? 'L' : 'l';
                }
                case 'L', 'l' -> {
                    if (i + 1 >= tokens.size()) {
                        return;
                    }
                    double x = parseDouble(tokens.get(i));
                    double y = parseDouble(tokens.get(i + 1));
                    i += 2;
                    if (command == 'l') {
                        x += currX;
                        y += currY;
                    }
                    currX = x;
                    currY = y;
                    plt.pd(currX, currY);
                }
                case 'H', 'h' -> {
                    if (i >= tokens.size()) {
                        return;
                    }
                    double x = parseDouble(tokens.get(i++));
                    if (command == 'h') {
                        x += currX;
                    }
                    currX = x;
                    plt.pd(currX, currY);
                }
                case 'V', 'v' -> {
                    if (i >= tokens.size()) {
                        return;
                    }
                    double y = parseDouble(tokens.get(i++));
                    if (command == 'v') {
                        y += currY;
                    }
                    currY = y;
                    plt.pd(currX, currY);
                }
                case 'Z', 'z' -> {
                    currX = startX;
                    currY = startY;
                    plt.pd(currX, currY);
                }
                case 'C', 'c' -> {
                    if (i + 5 >= tokens.size()) {
                        return;
                    }

                    double x1 = parseDouble(tokens.get(i));
                    double y1 = parseDouble(tokens.get(i + 1));
                    double x2 = parseDouble(tokens.get(i + 2));
                    double y2 = parseDouble(tokens.get(i + 3));
                    double x = parseDouble(tokens.get(i + 4));
                    double y = parseDouble(tokens.get(i + 5));
                    i += 6;

                    if (command == 'c') {
                        x1 += currX;
                        y1 += currY;
                        x2 += currX;
                        y2 += currY;
                        x += currX;
                        y += currY;
                    }

                    appendCubicBezier(plt, currX, currY, x1, y1, x2, y2, x, y);
                    currX = x;
                    currY = y;
                }
                default -> {
                    return;
                }
            }
        }
    }

    private static void appendCubicBezier(PltBuilder plt,
                                          double p0x,
                                          double p0y,
                                          double p1x,
                                          double p1y,
                                          double p2x,
                                          double p2y,
                                          double p3x,
                                          double p3y) {
        int segments = 24;
        for (int s = 1; s <= segments; s++) {
            double t = (double) s / segments;
            double mt = 1.0 - t;

            double x = mt * mt * mt * p0x
                    + 3 * mt * mt * t * p1x
                    + 3 * mt * t * t * p2x
                    + t * t * t * p3x;

            double y = mt * mt * mt * p0y
                    + 3 * mt * mt * t * p1y
                    + 3 * mt * t * t * p2y
                    + t * t * t * p3y;

            plt.pd(x, y);
        }
    }

    private static List<String> tokenizePath(String d) {
        List<String> tokens = new ArrayList<>();
        StringBuilder number = new StringBuilder();

        for (int i = 0; i < d.length(); i++) {
            char c = d.charAt(i);
            if (Character.isLetter(c)) {
                flushNumber(number, tokens);
                tokens.add(String.valueOf(c));
            } else if (c == '-' || c == '+' || Character.isDigit(c) || c == '.') {
                if ((c == '-' || c == '+') && !number.isEmpty()
                        && number.charAt(number.length() - 1) != 'e'
                        && number.charAt(number.length() - 1) != 'E') {
                    flushNumber(number, tokens);
                }
                number.append(c);
            } else if (c == ',' || Character.isWhitespace(c)) {
                flushNumber(number, tokens);
            }
        }
        flushNumber(number, tokens);
        return tokens;
    }

    private static void flushNumber(StringBuilder number, List<String> tokens) {
        if (!number.isEmpty()) {
            tokens.add(number.toString());
            number.setLength(0);
        }
    }

    private static boolean isCommand(String token) {
        return token.length() == 1 && Character.isLetter(token.charAt(0));
    }

    private static List<Point> parsePoints(String pointsAttr) {
        List<Point> points = new ArrayList<>();
        if (pointsAttr == null || pointsAttr.isBlank()) {
            return points;
        }
        String[] values = pointsAttr.trim().split("[\\s,]+");
        for (int i = 0; i + 1 < values.length; i += 2) {
            points.add(new Point(parseDouble(values[i]), parseDouble(values[i + 1])));
        }
        return points;
    }

    private static double getDouble(Element element, String name, double defaultValue) {
        String value = element.getAttribute(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return parseDouble(value);
    }

    private static double parseDouble(String text) {
        String normalized = text.trim().replaceAll("[a-zA-Z%]+$", "");
        if (normalized.isEmpty()) {
            return 0;
        }
        return Double.parseDouble(normalized);
    }

    private static String format(double value) {
        return NUMBER_FORMAT.format(value);
    }

    private record Point(double x, double y) {
    }

    private static final class PltBuilder {
        private final StringBuilder builder = new StringBuilder();

        private PltBuilder append(String command) {
            builder.append(command).append(';');
            return this;
        }

        private PltBuilder pu(double x, double y) {
            builder.append("PU").append(format(x)).append(',').append(format(y)).append(';');
            return this;
        }

        private PltBuilder pd(double x, double y) {
            builder.append("PD").append(format(x)).append(',').append(format(y)).append(';');
            return this;
        }

        private PltBuilder ci(double r) {
            builder.append("CI").append(format(r)).append(';');
            return this;
        }

        private String build() {
            return builder.toString();
        }
    }
}

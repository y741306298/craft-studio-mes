package com.mes.interfaces.api.test;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;
import java.util.*;

public class SVG2PLTConverter {

    // 缩放因子：SVG单位到HPGL单位（假设1 SVG单位 = 1/96 英寸，HPGL单位为0.025mm）
    // 1 英寸 = 25.4 mm，1 HPGL单位 = 0.025 mm，因此1英寸 = 1016 HPGL单位
    // 1 SVG单位通常定义为1/96英寸，所以 1 SVG单位 = (1/96)*1016 ≈ 10.5833 HPGL单位
    private static final double SVG_TO_HPGL = 10.5833;

    // 贝塞尔曲线细分精度（控制线段数量）
    private static final double CURVE_FLATNESS = 0.1; // 单位：HPGL单位

    public static void main(String[] args) {

        String svgFile = "D:\\cailiaobao\\svgtest\\未命名 -2.svg";
        String pltFile = "D:\\cailiaobao\\svgtest\\222222.plt";

        try {
            convertSVGtoPLT(svgFile, pltFile);
            System.out.println("转换成功: " + pltFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void convertSVGtoPLT(String svgPath, String pltPath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(svgPath));

        // 获取根元素，处理命名空间
        Element svgRoot = doc.getDocumentElement();

        // 解析SVG尺寸和viewBox
        double[] viewBox = parseViewBox(svgRoot);
        double svgWidth = parseLength(svgRoot.getAttribute("width"), viewBox[2]);
        double svgHeight = parseLength(svgRoot.getAttribute("height"), viewBox[3]);

        // 确定绘图区域：使用viewBox或尺寸
        double minX = viewBox[0];
        double minY = viewBox[1];
        double width = viewBox[2];
        double height = viewBox[3];

        // 收集所有路径数据
        List<List<double[]>> allSegments = new ArrayList<>();

        NodeList pathNodes = svgRoot.getElementsByTagNameNS("*", "path");
        for (int i = 0; i < pathNodes.getLength(); i++) {
            Element pathElem = (Element) pathNodes.item(i);
            String d = pathElem.getAttribute("d");
            if (d != null && !d.isEmpty()) {
                // 解析路径并转换为线段
                List<double[]> points = parsePathData(d);
                // 应用变换（如果有）
                String transform = pathElem.getAttribute("transform");
                if (transform != null && !transform.isEmpty()) {
                    applyTransform(points, transform);
                }
                if (!points.isEmpty()) {
                    allSegments.add(points);
                }
            }
        }

        // 生成HPGL文件
        writePLT(pltPath, allSegments, minX, minY, width, height);
    }

    /**
     * 解析viewBox属性，返回 [minX, minY, width, height]
     */
    private static double[] parseViewBox(Element svgRoot) {
        String viewBoxStr = svgRoot.getAttribute("viewBox");
        if (viewBoxStr != null && !viewBoxStr.isEmpty()) {
            String[] parts = viewBoxStr.trim().split("\\s+");
            if (parts.length == 4) {
                return new double[] {
                        Double.parseDouble(parts[0]),
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3])
                };
            }
        }
        // 默认viewBox为0 0 width height，但需要先有width/height
        double w = parseLength(svgRoot.getAttribute("width"), 100);
        double h = parseLength(svgRoot.getAttribute("height"), 100);
        return new double[] {0, 0, w, h};
    }

    /**
     * 解析长度（可能包含单位px）
     */
    private static double parseLength(String value, double defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        value = value.trim();
        if (value.endsWith("px")) {
            value = value.substring(0, value.length() - 2);
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 解析路径数据 d 属性，返回点列表（每个子路径以null分隔）
     * 这里简化：将路径转换为一系列直线段，每个子路径结束后用null标记
     */
    private static List<double[]> parsePathData(String d) {
        List<double[]> points = new ArrayList<>();
        // 正则匹配命令和参数
        String[] tokens = d.replaceAll(",", " ").split("\\s+");
        List<String> tokenList = new ArrayList<>();
        for (String t : tokens) {
            if (!t.isEmpty()) tokenList.add(t);
        }

        double currentX = 0, currentY = 0;
        double startX = 0, startY = 0;
        char lastCommand = ' ';

        int idx = 0;
        while (idx < tokenList.size()) {
            String token = tokenList.get(idx);
            char cmd = token.charAt(0);
            if (Character.isLetter(cmd)) {
                idx++;
            } else {
                // 如果没有命令，重复上一个命令（对于相对坐标）
                cmd = lastCommand;
                // 但不移动idx，继续用当前token作为参数开始
            }

            boolean isRelative = Character.isLowerCase(cmd);
            cmd = Character.toUpperCase(cmd);

            switch (cmd) {
                case 'M': // moveto
                case 'L': // lineto
                case 'H': // horizontal lineto
                case 'V': // vertical lineto
                case 'C': // curveto
                case 'S': // smooth curveto
                case 'Q': // quadratic Bezier
                case 'T': // smooth quadratic Bezier
                case 'A': // arc
                case 'Z': // closepath
                    // 这里只实现基本的M, L, Z，其他曲线近似为直线段
                    // 完整实现需要复杂的曲线细分
                    // 作为示例，我们只处理M, L, Z
                    // 对于C等曲线，简单取端点
                    break;
            }

            // 简单实现：只处理M, L, Z
            if (cmd == 'M' || cmd == 'L' || cmd == 'Z') {
                if (cmd == 'M') {
                    // 如果有前一个子路径未闭合，添加null分隔
                    if (!points.isEmpty() && points.get(points.size()-1) != null) {
                        points.add(null); // 子路径分隔
                    }
                    double x = Double.parseDouble(tokenList.get(idx++));
                    double y = Double.parseDouble(tokenList.get(idx++));
                    if (isRelative) {
                        x += currentX;
                        y += currentY;
                    }
                    startX = x;
                    startY = y;
                    currentX = x;
                    currentY = y;
                    points.add(new double[]{x, y});
                } else if (cmd == 'L') {
                    while (idx + 1 <= tokenList.size()) {
                        double x = Double.parseDouble(tokenList.get(idx++));
                        double y = Double.parseDouble(tokenList.get(idx++));
                        if (isRelative) {
                            x += currentX;
                            y += currentY;
                        }
                        currentX = x;
                        currentY = y;
                        points.add(new double[]{x, y});
                        if (idx >= tokenList.size() || Character.isLetter(tokenList.get(idx).charAt(0))) {
                            break;
                        }
                    }
                } else if (cmd == 'Z') {
                    // 闭合路径：回到起点
                    if (currentX != startX || currentY != startY) {
                        points.add(new double[]{startX, startY});
                        currentX = startX;
                        currentY = startY;
                    }
                    points.add(null); // 子路径结束
                    idx++; // 跳过Z本身（如果存在）
                }
            } else {
                // 其他命令简单跳过（仅示例）
                // 实际应解析参数个数
                if (cmd == 'C') idx += 6;
                else if (cmd == 'S') idx += 4;
                else if (cmd == 'Q') idx += 4;
                else if (cmd == 'T') idx += 2;
                else if (cmd == 'A') idx += 7;
                else {
                    // 未知命令，跳出循环
                    break;
                }
            }
            lastCommand = cmd;
        }
        return points;
    }

    /**
     * 应用变换（简化实现，仅支持translate和scale）
     */
    private static void applyTransform(List<double[]> points, String transform) {
        // 解析transform属性
        // 示例仅处理 translate(tx, ty) 和 scale(sx, sy)
        if (transform.contains("translate")) {
            String t = transform.replaceAll(".*translate\\(([^)]+)\\).*", "$1");
            String[] parts = t.split(",");
            double tx = Double.parseDouble(parts[0].trim());
            double ty = parts.length > 1 ? Double.parseDouble(parts[1].trim()) : 0;
            for (double[] p : points) {
                if (p != null) {
                    p[0] += tx;
                    p[1] += ty;
                }
            }
        }
        if (transform.contains("scale")) {
            String s = transform.replaceAll(".*scale\\(([^)]+)\\).*", "$1");
            String[] parts = s.split(",");
            double sx = Double.parseDouble(parts[0].trim());
            double sy = parts.length > 1 ? Double.parseDouble(parts[1].trim()) : sx;
            for (double[] p : points) {
                if (p != null) {
                    p[0] *= sx;
                    p[1] *= sy;
                }
            }
        }
    }

    /**
     * 写入HPGL文件
     */
    private static void writePLT(String pltPath, List<List<double[]>> allSegments,
                                 double minX, double minY, double width, double height) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(pltPath))) {
            // HPGL头部
            writer.println("IN;");          // 初始化
            writer.println("SP1;");          // 选择笔1

            // 将所有坐标转换为HPGL单位并平移使原点在左下角
            for (List<double[]> segments : allSegments) {
                boolean firstPoint = true;
                double prevX = 0, prevY = 0;

                for (double[] point : segments) {
                    if (point == null) {
                        // 子路径结束，抬笔
                        writer.println("PU;");
                        firstPoint = true;
                        continue;
                    }

                    // 转换坐标：翻转Y轴（SVG Y向下，HPGL Y向上）
                    double hpglX = (point[0] - minX) * SVG_TO_HPGL;
                    double hpglY = (height - (point[1] - minY)) * SVG_TO_HPGL; // 翻转Y

                    if (firstPoint) {
                        // 移动到起始点
                        writer.printf("PU%d,%d;\n", (int)Math.round(hpglX), (int)Math.round(hpglY));
                        firstPoint = false;
                    } else {
                        // 画线到当前点
                        writer.printf("PD%d,%d;\n", (int)Math.round(hpglX), (int)Math.round(hpglY));
                    }

                    prevX = hpglX;
                    prevY = hpglY;
                }
                // 子路径结束后抬笔
                writer.println("PU;");
            }

            // HPGL尾部
            writer.println("SP0;");          // 抬笔
            writer.println("IN;");            // 结束
        }
    }
}
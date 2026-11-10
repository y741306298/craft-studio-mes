package com.mes.application.command.orderPreprocessing.sublimation;

import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** 热升华名称模糊匹配，后续工艺名称精确匹配；汇总后只生成一套出血参数。 */
public final class SublimationRules {
    private static final Set<String> SPECIAL_NAMES = Set.of("缝管套", "缝布带管套", "单针锁边", "双针锁边",
            "三线锁边", "四线锁边", "双面缝", "双面夹黑缝", "旗帜四角打扣", "旗帜打扣", "缝绑带");
    private static final Pattern SLEEVE_SPEC = Pattern.compile("^(白色|彩色)直径([0-9]+(?:\\.[0-9]+)?)\\s*cm$", Pattern.CASE_INSENSITIVE);

    public record Step(String name, Integer order, List<JSONObject> params) {
        public Step { params = List.copyOf(params); }
    }

    private SublimationRules() { }

    private static List<Step> followingNodes(List<Step> flow) {
        if (flow == null) return List.of();
        List<Step> nodes = flow.stream().filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(Step::order,
                        Comparator.nullsLast(Comparator.naturalOrder()))).toList();
        for (int i = 0; i < nodes.size(); i++) {
            String name = nodes.get(i).name();
            if (name != null && name.contains("热升华")) return nodes.subList(i + 1, nodes.size());
        }
        return List.of();
    }

    public static boolean applies(List<Step> flow) {
        if (flow == null || flow.stream().noneMatch(n -> n != null
                && n.name() != null && n.name().contains("热升华"))) return false;
        List<Step> following = followingNodes(flow);
        // 兼容原有矩形旗缝套筒入口；只有热升华而未配置缝套的新流程会报出明确配置错误。
        boolean oldRectangle = following.stream().anyMatch(n -> n.name() != null
                && n.name().contains("矩形旗缝套筒"));
        return !oldRectangle || following.stream().anyMatch(n -> SPECIAL_NAMES.contains(n.name() == null ? "" : n.name()));
    }

    public static SublimationPlan resolve(List<Step> allNodes, boolean containsShapeOrSplice) {
        if (!applies(allNodes)) return null;
        List<Step> nodes = followingNodes(allNodes);
        SublimationGeometry.Edge sleeve = null;
        int sleeveWidth = 0;
        String sleeveColor = null;
        int sewWidth = 0;
        boolean corners = false;
        Set<SublimationGeometry.Edge> buckles = EnumSet.noneOf(SublimationGeometry.Edge.class);
        Set<SublimationGeometry.Edge> ties = EnumSet.noneOf(SublimationGeometry.Edge.class);
        SublimationDoubleSideJoin.Mode mode = SublimationDoubleSideJoin.Mode.NONE;
        List<String> names = new ArrayList<>();
        for (int index = 0; index < nodes.size(); index++) {
            Step node = nodes.get(index);
            String name = node.name();
            if (name == null || !SPECIAL_NAMES.contains(name)) continue;
            names.add(name);
            switch (name) {
                case "缝管套", "缝布带管套" -> {
                    Set<SublimationGeometry.Edge> edges = selectedEdges(node);
                    if (edges.size() != 1) throw new IllegalArgumentException(name + "必须指定一条套筒边");
                    SublimationGeometry.Edge edge = edges.iterator().next();
                    SleeveSpec spec = "缝布带管套".equals(name) ? new SleeveSpec(10, "#ffffff")
                            : followingSleeveSpec(nodes, index);
                    int width = spec.widthMm();
                    if (sleeve != null && (sleeve != edge || sleeveWidth != width
                            || !java.util.Objects.equals(sleeveColor, spec.colorHex()))) {
                        throw new IllegalArgumentException("矩形旗出血算法仅支持一条套筒边及一种直径，缝套工艺参数冲突");
                    }
                    sleeve = edge;
                    sleeveWidth = width;
                    sleeveColor = spec.colorHex();
                }
                case "单针锁边" -> sewWidth = Math.max(sewWidth, 20);
                case "双针锁边", "三线锁边", "四线锁边" -> sewWidth = Math.max(sewWidth, 30);
                case "旗帜四角打扣" -> corners = true;
                case "旗帜打扣" -> buckles.addAll(selectedEdges(node));
                case "缝绑带" -> ties.addAll(selectedEdges(node));
                case "双面缝", "双面夹黑缝" -> {
                    var next = "双面缝".equals(name) ? SublimationDoubleSideJoin.Mode.DOUBLE_SEWN
                            : SublimationDoubleSideJoin.Mode.BLACK_INTERLAYER;
                    if (mode != SublimationDoubleSideJoin.Mode.NONE && mode != next) {
                        throw new IllegalArgumentException("双面缝和双面夹黑缝不能同时指定");
                    }
                    mode = next;
                }
                default -> throw new IllegalStateException("未处理的热升华后续工艺：" + name);
            }
        }
        if (sleeve == null) throw new IllegalArgumentException("热升华组合工艺必须包含缝管套或缝布带管套");
        // 当前仅支持矩形旗帜，不静默丢弃异形/拼接工艺。
        if (containsShapeOrSplice) {
            throw new IllegalArgumentException("热升华组合策略暂不支持异形或拼接旗帜");
        }
        String backUrl = resolveBackImage(nodes, mode);
        return new SublimationPlan(sleeve, sleeveWidth, sewWidth, corners, buckles, ties, mode, backUrl, names, sleeveColor);
    }

    /** 与矩形旗缝套筒一致，只读取 param.type=RECT_EDGE_SELECTION 下的 edges。 */
    private static Set<SublimationGeometry.Edge> selectedEdges(Step node) {
        Set<SublimationGeometry.Edge> result = EnumSet.noneOf(SublimationGeometry.Edge.class);
        for (JSONObject param : node.params()) {
            if (!"RECT_EDGE_SELECTION".equals(param.getString("type"))) continue;
            Object raw = param.get("edges");
            if (!(raw instanceof List<?> edges)) throw new IllegalArgumentException(node.name() + "缺少有效的 param.edges 列表");
            for (Object edge : edges) {
                try {
                    result.add(SublimationGeometry.Edge.valueOf(String.valueOf(edge).toUpperCase(java.util.Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(node.name() + "选边必须为 LEFT/TOP/BOTTOM/RIGHT", e);
                }
            }
        }
        if (result.isEmpty()) throw new IllegalArgumentException(node.name() + "缺少 RECT_EDGE_SELECTION 选边配置");
        return result;
    }

    private record SleeveSpec(int widthMm, String colorHex) { }

    /** 规格是缝管套后续的独立工艺节点，不再读取配件名称。下一缝套节点开始新的作用域。 */
    private static SleeveSpec followingSleeveSpec(List<Step> nodes, int sleeveIndex) {
        SleeveSpec result = null;
        for (int i = sleeveIndex + 1; i < nodes.size(); i++) {
            String name = nodes.get(i).name();
            if ("缝管套".equals(name) || "缝布带管套".equals(name)) break;
            if (name == null) continue;
            var matcher = SLEEVE_SPEC.matcher(name);
            if (!matcher.matches()) continue;
            int mm;
            try {
                mm = new java.math.BigDecimal(matcher.group(2)).multiply(java.math.BigDecimal.TEN).intValueExact();
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("缝管套直径换算后必须为整数毫米且不能超出范围", e);
            }
            if (mm <= 0) throw new IllegalArgumentException("缝管套直径必须大于0");
            SleeveSpec spec = new SleeveSpec(mm, "白色".equals(matcher.group(1)) ? "#ffffff" : null);
            if (result != null && !result.equals(spec)) {
                throw new IllegalArgumentException("缝管套后续存在冲突的直径或颜色工艺");
            }
            result = spec;
        }
        if (result == null) throw new IllegalArgumentException("缝管套后续缺少白色直径Xcm或彩色直径Xcm工艺");
        return result;
    }

    private static String resolveBackImage(List<Step> nodes, SublimationDoubleSideJoin.Mode mode) {
        if (mode == SublimationDoubleSideJoin.Mode.NONE) return null;
        String back = null;
        boolean same = false;
        boolean different = false;
        for (Step node : nodes) {
            if ("反面相同画面".equals(node.name())) same = true;
            if (!"反面不同画面".equals(node.name())) continue;
            different = true;
            for (JSONObject param : node.params()) {
                JSONObject file = param.getJSONObject("file");
                if (file == null) continue;
                String url = file.getString("rawFile");
                if (url == null || url.isBlank()) {
                    JSONObject preview = file.getJSONObject("filePreview");
                    url = preview == null ? null : preview.getString("raw");
                }
                if (url != null && !url.isBlank()) back = url;
            }
        }
        if (same && different) throw new IllegalArgumentException("反面相同画面和反面不同画面不能同时指定");
        if (different && back == null) throw new IllegalArgumentException("反面不同画面缺少图片URL");
        if (!same && !different) throw new IllegalArgumentException("双面缝/双面夹黑缝必须指定反面相同画面或反面不同画面");
        // null 代表复用正面，由矩形出血算法的 mirrorRequired 输出镜像面。
        return back;
    }

}

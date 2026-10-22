package com.mes.application.command.typesetting.proces.reshenghua;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 工艺节点后向后查找颜色直径，再读取其后的连续方向节点；直径仅保存在本次调用中。 */
record SublimationSewingSpec(String color, double diameterCm, List<SewingDirection> directions) {
    private static final Pattern DIAMETER = Pattern.compile("^(.*?)直径\\s*((?:\\d+(?:\\.\\d+)?|\\.\\d+))\\s*cm$", Pattern.CASE_INSENSITIVE);

    static boolean isSewingNode(String name) {
        return "缝管套".equals(name == null ? null : name.trim())
                || "道旗缝边".equals(name == null ? null : name.trim());
    }

    static List<SublimationSewingSpec> parse(List<String> nodes) {
        List<SublimationSewingSpec> result = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            if (!isSewingNode(nodes.get(i))) {
                continue;
            }
            Matcher matcher = DIAMETER.matcher("");
            int cursor = i + 1;
            for (; cursor < nodes.size(); cursor++) {
                String nodeName = nodes.get(cursor);
                matcher.reset(nodeName == null ? "" : nodeName.trim());
                if (matcher.matches()) {
                    break;
                }
            }
            if (cursor == nodes.size()) break;
            String diameterNode = nodes.get(cursor);
            double diameter = Double.parseDouble(matcher.group(2));
            if (!Double.isFinite(diameter) || diameter <= 0D) {
                throw new IllegalArgumentException("热升华直径必须为正数: " + diameterNode);
            }
            List<SewingDirection> directions = new ArrayList<>();
            cursor++;
            while (cursor < nodes.size()) {
                SewingDirection direction = SewingDirection.match(nodes.get(cursor));
                if (direction == null) {
                    break;
                }
                directions.add(direction);
                cursor++;
            }
            if (directions.isEmpty()) {
                throw new IllegalArgumentException(diameterNode + "后必须紧接缝边方向节点");
            }
            result.add(new SublimationSewingSpec(matcher.group(1).trim(), diameter, List.copyOf(directions)));
            i = cursor - 1;
        }
        return result;
    }

    static Map<SewingDirection.Edge, Double> margins(List<SublimationSewingSpec> specs) {
        Map<SewingDirection.Edge, Double> margins = new EnumMap<>(SewingDirection.Edge.class);
        for (SublimationSewingSpec spec : specs) {
            double distanceMm = Math.PI * spec.diameterCm * 10D;
            if (!Double.isFinite(distanceMm)) {
                throw new IllegalArgumentException("热升华外扩距离超出范围");
            }
            for (SewingDirection direction : spec.directions) {
                for (SewingDirection.Edge edge : direction.edges()) {
                    // 同一边重复声明只取最大需求，不因重复方向节点累计外扩。
                    margins.merge(edge, distanceMm, Math::max);
                }
            }
        }
        return margins;
    }
}

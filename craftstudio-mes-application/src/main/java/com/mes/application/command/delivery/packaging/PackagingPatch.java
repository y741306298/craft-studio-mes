package com.mes.application.command.delivery.packaging;

import java.util.*;

/** Pure patch builder: never replaces a production-piece document or the procedure-flow array. */
public final class PackagingPatch {
    private PackagingPatch() {}
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> nodes(Map<String, Object> piece) {
        Object flow = piece.get("procedureFlow");
        return flow instanceof Map<?, ?> m && m.get("nodes") instanceof List<?> nodes
                ? (List<Map<String, Object>>) nodes : List.of();
    }
    private static int integer(Map<String, Object> value, String key, int fallback) {
        return value.get(key) instanceof Number n ? n.intValue() : fallback;
    }
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> infos(Map<String, Object> piece) {
        return piece.get("deliveryPkgInfos") instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }
    public static int quantity(Map<String, Object> node) {
        Number value = (Number) node.get("pieceQuantity");
        return value == null ? 0 : value.intValue();
    }
    public static int index(List<Map<String, Object>> nodes, String name) {
        for (int i = 0; i < nodes.size(); i++) if (name.equals(nodes.get(i).get("nodeName"))) return i;
        throw new IllegalStateException("Missing packaging node: " + name);
    }
    public static Map<String, Object> transfer(Map<String, Object> piece, int quantity, Integer required, Map<String, Object> pkg, String completed) {
        List<Map<String, Object>> nodes = nodes(piece);
        int pending = index(nodes, "待打包"), packed = index(nodes, "已打包");
        if (quantity <= 0 || quantity > quantity(nodes.get(pending)))
            throw new IllegalStateException("Packaging quantity no longer available");
        int total = integer(piece, "quantity", quantity(nodes.get(packed)) + quantity);
        int packedQty = Math.min(quantity(nodes.get(packed)) + quantity, Math.max(total, 0));
        Map<String, Object> changes = new LinkedHashMap<>();
        put(changes, piece, "procedureFlow.nodes." + pending + ".pieceQuantity", quantity(nodes.get(pending)) - quantity);
        if (quantity(nodes.get(pending)) == quantity)
            put(changes, piece, "procedureFlow.nodes." + pending + ".nodeStatus", "COMPLETED");
        put(changes, piece, "procedureFlow.nodes." + packed + ".pieceQuantity", packedQty);
        put(changes, piece, "procedureFlow.nodes." + packed + ".nodeStatus", "ACTIVE");
        int threshold = required == null || required <= 0 ? total : required;
        if (threshold > 0 && packedQty >= threshold && !completed.equals(piece.get("status"))) {
            put(changes, piece, "status", completed);
            for (int i = 0; i < nodes.size(); i++)
                put(changes, piece, "procedureFlow.nodes." + i + ".pieceQuantity", i == packed ? total : 0);
        }
        List<Map<String, Object>> infos = new ArrayList<>();
        for (Map<String, Object> info : infos(piece)) infos.add(new LinkedHashMap<>(info));
        Map<String, Object> info = infos.stream().filter(d -> Objects.equals(d.get("carrierId"), pkg.get("carrierId"))
                && Objects.equals(d.get("kuaidiNum"), pkg.get("kuaidiNum"))).findFirst().orElse(null);
        if (info == null) {
            info = new LinkedHashMap<>();
            info.put("carrierId", pkg.get("carrierId"));
            info.put("kuaidiNum", pkg.get("kuaidiNum"));
            infos.add(info);
        }
        info.put("quantity", integer(info, "quantity", 0) + quantity);
        String carrier = (String) pkg.get("carrierName");
        if (carrier != null && !carrier.isBlank()) {
            String route = (String) pkg.get("routeId"), node = (String) pkg.get("routeNodeId");
            info.put("carrierName", carrier + (route == null || route.isBlank() || node == null || node.isBlank()
                    ? "(未自定义路线)" : "(" + route + "/" + node + ")"));
        }
        changes.put("deliveryPkgInfos", infos);
        return changes;
    }
    public static boolean fullyPacked(Map<String, Object> piece, int required) {
        if (required <= 0) return false;
        return nodes(piece).stream().filter(d -> "已打包".equals(d.get("nodeName")))
                .anyMatch(d -> quantity(d) >= required);
    }
    public static void put(Map<String, Object> changes, Map<String, Object> before, String path, Object value) {
        if (!Objects.equals(valueAt(before, path), value)) changes.put(path, value);
        else changes.remove(path);
    }
    private static Object valueAt(Object value, String path) {
        for (String part : path.split("\\.")) {
            if (value instanceof Map<?, ?> d) value = d.get(part);
            else if (value instanceof List<?> list) value = list.get(Integer.parseInt(part));
            else return null;
        }
        return value;
    }
}

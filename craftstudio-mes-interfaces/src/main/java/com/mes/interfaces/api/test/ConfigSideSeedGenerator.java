package com.mes.interfaces.api.test;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConfigSideSeedGenerator {
    private static final List<String> DEVICE_TYPES = List.of("print", "cutting", "fuban");
    private static final List<String> MANUFACTURER_TYPES = List.of("basic_print", "standard_product", "character_card");

    public static void main(String[] args) {
        try {
            Config config = Config.parse(args);
            if (config.help) {
                Config.printHelp();
                return;
            }

            ApiClient client = new ApiClient(config);
            Generator generator = new Generator(client);
            generator.run();
            System.out.println("全部数据生成完成");
        } catch (IllegalArgumentException ex) {
            System.err.println("参数错误: " + ex.getMessage());
            Config.printHelp();
            System.exit(1);
        } catch (Exception ex) {
            System.err.println("执行失败: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static final class Generator {
        private final ApiClient client;
        private final Config config;
        private final List<String> deviceIds = new ArrayList<>();
        private final List<String> procedureIds = new ArrayList<>();

        private Generator(ApiClient client) {
            this.client = client;
            this.config = client.config;
        }

        private void run() throws Exception {
            createDevices(config.deviceCount);
            createProcedures(config.procedureCount);
            createProcedureFlows(config.flowCount);
            createFactories(config.factoryCount);
        }

        private void createDevices(int count) throws Exception {
            for (int index = 1; index <= count; index++) {
                String deviceType = DEVICE_TYPES.get((index - 1) % DEVICE_TYPES.size());
                String deviceId = String.format(Locale.ROOT, "dev-%04d", index);

                Map<String, Object> payload = orderedMap();
                payload.put("deviceInfoId", deviceId);
                payload.put("deviceInfoName", String.format(Locale.ROOT, "设备-%04d", index));
                payload.put("deviceType", deviceType);
                payload.put("capacity", String.valueOf(100 + index));
                payload.put("unit", "pcs");
                payload.put("brand", "Brand-" + (((index - 1) % 10) + 1));
                payload.put("maxWeight", 100.0 + index);
                payload.put("maxHegiht", 20.0 + (index % 30));
                payload.put("materials", "材料-" + (((index - 1) % 8) + 1));
                payload.put("deviceProcedures", List.of());
                payload.put("deviceMaterials", List.of());

                client.postJson("/api/configSide/device/add", payload, "创建设备 " + deviceId);
                deviceIds.add(deviceId);
                if (index % 20 == 0 || index == count) {
                    System.out.printf("[device] 已创建 %d/%d%n", index, count);
                }
            }
        }

        private void createProcedures(int count) throws Exception {
            for (int index = 1; index <= count; index++) {
                String procedureId = String.format(Locale.ROOT, "proc-%04d", index);
                String deviceType = DEVICE_TYPES.get((index - 1) % DEVICE_TYPES.size());

                Map<String, Object> payload = orderedMap();
                payload.put("procedureId", procedureId);
                payload.put("procedureName", String.format(Locale.ROOT, "工序-%04d", index));
                payload.put("procedureType", "type-" + (((index - 1) % 20) + 1));
                payload.put("deviceType", deviceType);
                payload.put("status", "enabled");
                payload.put("scriptUrl", "https://example.local/scripts/" + procedureId + ".groovy");
                payload.put("remarks", "批量初始化工序 " + index);

                client.postJson("/api/configSide/procedure/add", payload, "创建工序 " + procedureId);
                procedureIds.add(procedureId);
                if (index % 100 == 0 || index == count) {
                    System.out.printf("[procedure] 已创建 %d/%d%n", index, count);
                }
            }
        }

        private void createProcedureFlows(int count) throws Exception {
            if (procedureIds.isEmpty()) {
                throw new IllegalStateException("创建工序流程前必须先创建工序");
            }

            int proceduresPerFlow = 5;
            for (int index = 1; index <= count; index++) {
                int start = ((index - 1) * proceduresPerFlow) % procedureIds.size();
                List<Object> nodes = new ArrayList<>();
                for (int offset = 0; offset < proceduresPerFlow; offset++) {
                    int order = offset + 1;
                    String procedureId = procedureIds.get((start + offset) % procedureIds.size());
                    Map<String, Object> node = orderedMap();
                    node.put("nodeId", String.format(Locale.ROOT, "flow-%04d-node-%02d", index, order));
                    node.put("nodeName", String.format(Locale.ROOT, "工序组%04d-节点-%02d", index, order));
                    node.put("procedureId", procedureId);
                    node.put("nodeOrder", order);
                    node.put("nodeType", "TASK");
                    node.put("description", "关联工序 " + procedureId);
                    node.put("remarks", "自动生成节点 " + order);
                    node.put("retryCount", 0);
                    node.put("pieceQuantity", 0);
                    nodes.add(node);
                }

                Map<String, Object> payload = orderedMap();
                payload.put("procedureFlowId", String.format(Locale.ROOT, "flow-%04d", index));
                payload.put("procedureFlowName", String.format(Locale.ROOT, "工序组-%04d", index));
                payload.put("flowDescription", String.format(Locale.ROOT, "自动生成工序组 %04d", index));
                payload.put("flowStatus", "DRAFT");
                payload.put("nodes", nodes);
                payload.put("totalNodes", nodes.size());

                client.postJson("/api/configSide/procedureFlow/add", payload, "创建工序组 flow-" + String.format(Locale.ROOT, "%04d", index));
                if (index % 20 == 0 || index == count) {
                    System.out.printf("[procedureFlow] 已创建 %d/%d%n", index, count);
                }
            }
        }

        private void createFactories(int count) throws Exception {
            if (deviceIds.isEmpty()) {
                throw new IllegalStateException("创建工厂前至少需要先创建设备");
            }

            int devicePointer = 0;
            for (int index = 1; index <= count; index++) {
                String manufacturerMetaId = String.format(Locale.ROOT, "factory-%03d", index);
                List<Object> workshops = new ArrayList<>();
                workshops.add(buildWorkshop(manufacturerMetaId, 1));
                workshops.add(buildWorkshop(manufacturerMetaId, 2));

                List<Object> factoryDevices = new ArrayList<>();
                for (int offset = 0; offset < 3; offset++) {
                    String deviceId = deviceIds.get((devicePointer + offset) % deviceIds.size());
                    int numericId = Integer.parseInt(deviceId.substring(deviceId.lastIndexOf('-') + 1));
                    String deviceType = DEVICE_TYPES.get((numericId - 1) % DEVICE_TYPES.size());
                    Map<String, Object> factoryDevice = orderedMap();
                    factoryDevice.put("manufacturerMetaId", manufacturerMetaId);
                    factoryDevice.put("deviceId", deviceId);
                    factoryDevice.put("deviceName", String.format(Locale.ROOT, "设备-%04d", numericId));
                    factoryDevice.put("deviceType", deviceType);
                    factoryDevice.put("deviceCode", String.format(Locale.ROOT, "%s-EQ-%d", manufacturerMetaId.toUpperCase(Locale.ROOT), offset + 1));
                    factoryDevice.put("capacity", 80.0 + numericId);
                    factoryDevice.put("capacityUnit", "pcs");
                    factoryDevices.add(factoryDevice);
                }
                devicePointer += 3;

                Map<String, Object> payload = orderedMap();
                payload.put("manufacturerMetaId", manufacturerMetaId);
                payload.put("manufacturerMetaType", MANUFACTURER_TYPES.get((index - 1) % MANUFACTURER_TYPES.size()));
                payload.put("name", String.format(Locale.ROOT, "工厂-%03d", index));
                payload.put("description", String.format(Locale.ROOT, "自动生成的工厂数据 %03d", index));
                payload.put("workshops", workshops);
                payload.put("deviceCfgs", factoryDevices);

                client.postJson("/api/configSide/manufactureMeta/add", payload, "创建工厂 " + manufacturerMetaId);
                if (index % 10 == 0 || index == count) {
                    System.out.printf("[factory] 已创建 %d/%d%n", index, count);
                }
            }
        }

        private Map<String, Object> buildWorkshop(String manufacturerMetaId, int workshopNo) {
            Map<String, Object> workshop = orderedMap();
            workshop.put("workshopId", String.format(Locale.ROOT, "%s-ws-%02d", manufacturerMetaId, workshopNo));
            workshop.put("workshopName", String.format(Locale.ROOT, "%s-车间-%02d", manufacturerMetaId, workshopNo));
            workshop.put("status", "enabled");
            workshop.put("productionLines", List.of());
            return workshop;
        }
    }

    private static final class ApiClient {
        private final Config config;
        private final HttpClient client;

        private ApiClient(Config config) {
            this.config = config;
            this.client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(config.timeoutSeconds))
                    .build();
        }

        private void postJson(String path, Map<String, Object> payload, String context) throws Exception {
            String url = buildUrl(path, Map.of());
            String body = Json.toJson(payload);
            if (config.dryRun) {
                System.out.println("[DRY-RUN] POST " + url);
                System.out.println(Json.prettyJson(payload));
                sleepIfNeeded();
                return;
            }

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(config.timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            for (Map.Entry<String, String> entry : config.headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }

            HttpResponse<String> response;
            try {
                response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (IOException | InterruptedException ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new RuntimeException("POST " + url + " 失败: " + ex.getMessage(), ex);
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + response.statusCode() + " -> POST " + url + " 失败: " + response.body());
            }

            ensureSuccess(response.body(), context);
            sleepIfNeeded();
        }

        private String buildUrl(String path, Map<String, String> query) {
            StringBuilder builder = new StringBuilder();
            builder.append(config.baseUrl.endsWith("/") ? config.baseUrl.substring(0, config.baseUrl.length() - 1) : config.baseUrl);
            builder.append(path.startsWith("/") ? path : "/" + path);
            if (!query.isEmpty()) {
                builder.append('?');
                boolean first = true;
                for (Map.Entry<String, String> entry : query.entrySet()) {
                    if (!first) {
                        builder.append('&');
                    }
                    first = false;
                    builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                    builder.append('=');
                    builder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                }
            }
            return builder.toString();
        }

        private void ensureSuccess(String responseBody, String context) {
            if (responseBody == null || responseBody.isBlank()) {
                return;
            }

            String compact = responseBody.replaceAll("\\s+", "");
            if (compact.contains("\"success\":false")) {
                throw new RuntimeException(context + " 失败: " + responseBody);
            }

            Integer code = extractIntegerField(compact, "code");
            if (code != null && code != 0 && code != 200) {
                throw new RuntimeException(context + " 失败: " + responseBody);
            }
        }

        private Integer extractIntegerField(String json, String fieldName) {
            String marker = "\"" + fieldName + "\":";
            int start = json.indexOf(marker);
            if (start < 0) {
                return null;
            }
            int valueStart = start + marker.length();
            int valueEnd = valueStart;
            while (valueEnd < json.length()) {
                char c = json.charAt(valueEnd);
                if ((c >= '0' && c <= '9') || (valueEnd == valueStart && c == '-')) {
                    valueEnd++;
                    continue;
                }
                break;
            }
            if (valueEnd == valueStart) {
                return null;
            }
            return Integer.parseInt(json.substring(valueStart, valueEnd));
        }

        private void sleepIfNeeded() {
            if (config.pauseMillis <= 0) {
                return;
            }
            try {
                Thread.sleep(config.pauseMillis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("等待被中断", ex);
            }
        }
    }

    private static final class Config {
        private final String baseUrl;
        private final Map<String, String> headers;
        private final int deviceCount;
        private final int procedureCount;
        private final int flowCount;
        private final int factoryCount;
        private final int timeoutSeconds;
        private final long pauseMillis;
        private final boolean dryRun;
        private final boolean help;

        private Config(String baseUrl, Map<String, String> headers, int deviceCount, int procedureCount,
                       int flowCount, int factoryCount, int timeoutSeconds, long pauseMillis,
                       boolean dryRun, boolean help) {
            this.baseUrl = baseUrl;
            this.headers = headers;
            this.deviceCount = deviceCount;
            this.procedureCount = procedureCount;
            this.flowCount = flowCount;
            this.factoryCount = factoryCount;
            this.timeoutSeconds = timeoutSeconds;
            this.pauseMillis = pauseMillis;
            this.dryRun = dryRun;
            this.help = help;
        }

        private static Config parse(String[] args) {
            String baseUrl = "http://127.0.0.1:8081";
            Map<String, String> headers = new LinkedHashMap<>();
            int deviceCount = 200;
            int procedureCount = 1000;
            int flowCount = 200;
            int factoryCount = 100;
            int timeoutSeconds = 30;
            long pauseMillis = 0L;
            boolean dryRun = false;
            boolean help = false;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--help", "-h" -> help = true;
                    case "--base-url" -> baseUrl = requireValue(args, ++i, arg);
                    case "--header" -> {
                        String header = requireValue(args, ++i, arg);
                        int splitIndex = header.indexOf(':');
                        if (splitIndex <= 0) {
                            throw new IllegalArgumentException("请求头格式必须为 Key:Value，当前值: " + header);
                        }
                        String key = header.substring(0, splitIndex).trim();
                        String value = header.substring(splitIndex + 1).trim();
                        headers.put(key, value);
                    }
                    case "--device-count" -> deviceCount = parseInt(requireValue(args, ++i, arg), arg);
                    case "--procedure-count" -> procedureCount = parseInt(requireValue(args, ++i, arg), arg);
                    case "--flow-count" -> flowCount = parseInt(requireValue(args, ++i, arg), arg);
                    case "--factory-count" -> factoryCount = parseInt(requireValue(args, ++i, arg), arg);
                    case "--timeout" -> timeoutSeconds = parseInt(requireValue(args, ++i, arg), arg);
                    case "--pause-ms" -> pauseMillis = parseLong(requireValue(args, ++i, arg), arg);
                    case "--dry-run" -> dryRun = true;
                    default -> throw new IllegalArgumentException("不支持的参数: " + arg);
                }
            }

            if (!help && (baseUrl == null || baseUrl.isBlank())) {
                throw new IllegalArgumentException("必须提供 --base-url");
            }

            return new Config(baseUrl, headers, deviceCount, procedureCount, flowCount, factoryCount,
                    timeoutSeconds, pauseMillis, dryRun, help);
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException(option + " 缺少参数值");
            }
            return args[index];
        }

        private static int parseInt(String value, String option) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(option + " 的值不是合法整数: " + value);
            }
        }

        private static long parseLong(String value, String option) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(option + " 的值不是合法整数: " + value);
            }
        }

        private static void printHelp() {
            System.out.println("用法:");
            System.out.println("  java scripts/ConfigSideSeedGenerator.java --base-url http://127.0.0.1:8080 [options]");
            System.out.println();
            System.out.println("参数:");
            System.out.println("  --base-url <url>         服务根地址，例如 http://127.0.0.1:8080");
            System.out.println("  --header <Key:Value>     附加请求头，可重复传入");
            System.out.println("  --device-count <n>       设备数量，默认 200");
            System.out.println("  --procedure-count <n>    工序数量，默认 1000");
            System.out.println("  --flow-count <n>         工序组/工序流程数量，默认 200");
            System.out.println("  --factory-count <n>      工厂数量，默认 100");
            System.out.println("  --timeout <seconds>      单次请求超时秒数，默认 30");
            System.out.println("  --pause-ms <millis>      每次请求后的暂停毫秒数，默认 0");
            System.out.println("  --dry-run                只打印请求，不真正发起调用");
            System.out.println("  --help, -h               打印帮助");
        }
    }

    private static final class Json {
        private Json() {
        }

        private static String toJson(Object value) {
            StringBuilder builder = new StringBuilder();
            writeJson(builder, value, 0, false);
            return builder.toString();
        }

        private static String prettyJson(Object value) {
            StringBuilder builder = new StringBuilder();
            writeJson(builder, value, 0, true);
            return builder.toString();
        }

        private static void writeJson(StringBuilder builder, Object value, int indent, boolean pretty) {
            if (value == null) {
                builder.append("null");
            } else if (value instanceof String string) {
                builder.append('"').append(escape(string)).append('"');
            } else if (value instanceof Number || value instanceof Boolean) {
                builder.append(value);
            } else if (value instanceof Map<?, ?> map) {
                writeObject(builder, map, indent, pretty);
            } else if (value instanceof List<?> list) {
                writeArray(builder, list, indent, pretty);
            } else {
                builder.append('"').append(escape(String.valueOf(value))).append('"');
            }
        }

        private static void writeObject(StringBuilder builder, Map<?, ?> map, int indent, boolean pretty) {
            builder.append('{');
            if (!map.isEmpty()) {
                int nextIndent = indent + 2;
                boolean first = true;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!first) {
                        builder.append(',');
                    }
                    if (pretty) {
                        builder.append('\n').append(" ".repeat(nextIndent));
                    }
                    builder.append('"').append(escape(String.valueOf(entry.getKey()))).append('"').append(':');
                    if (pretty) {
                        builder.append(' ');
                    }
                    writeJson(builder, entry.getValue(), nextIndent, pretty);
                    first = false;
                }
                if (pretty) {
                    builder.append('\n').append(" ".repeat(indent));
                }
            }
            builder.append('}');
        }

        private static void writeArray(StringBuilder builder, List<?> list, int indent, boolean pretty) {
            builder.append('[');
            if (!list.isEmpty()) {
                int nextIndent = indent + 2;
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) {
                        builder.append(',');
                    }
                    if (pretty) {
                        builder.append('\n').append(" ".repeat(nextIndent));
                    }
                    writeJson(builder, list.get(i), nextIndent, pretty);
                }
                if (pretty) {
                    builder.append('\n').append(" ".repeat(indent));
                }
            }
            builder.append(']');
        }

        private static String escape(String value) {
            return value
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }

    private static Map<String, Object> orderedMap() {
        return new LinkedHashMap<>();
    }
}

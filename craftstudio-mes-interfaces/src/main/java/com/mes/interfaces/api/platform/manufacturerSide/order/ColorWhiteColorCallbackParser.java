package com.mes.interfaces.api.platform.manufacturerSide.order;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mes.application.command.api.resp.ColorWhiteColorResponse;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** 彩白彩算法可能使用表单提交 code、message 和 JSON 字符串格式的 data。 */
final class ColorWhiteColorCallbackParser {
    private ColorWhiteColorCallbackParser() {
    }

    static ColorWhiteColorResponse parse(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            throw new IllegalArgumentException("彩白彩回调报文不能为空");
        }
        String body = payload.trim();
        // 同时兼容正确 JSON Content-Type 和误标为表单的原始 JSON。
        if (body.startsWith("{")) {
            return JSON.parseObject(body, ColorWhiteColorResponse.class);
        }
        JSONObject form = new JSONObject();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length == 2
                    ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            if (form.containsKey(key)) {
                throw new IllegalArgumentException("彩白彩回调表单字段重复：" + key);
            }
            form.put(key, value);
        }
        String data = form.getString("data");
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("彩白彩回调表单缺少data");
        }
        // 解析嵌套对象，避免将 JSON 字符串直接映射成 Data 对象。
        form.put("data", JSON.parseObject(data));
        return JSON.parseObject(form.toJSONString(), ColorWhiteColorResponse.class);
    }
}

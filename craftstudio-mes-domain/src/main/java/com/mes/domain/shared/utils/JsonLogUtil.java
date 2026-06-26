package com.mes.domain.shared.utils;

import com.alibaba.fastjson2.JSON;

/**
 * Utility methods for log-only JSON serialization.
 *
 * <p>Business logic must not depend on this class for payload persistence or API
 * serialization. It is intentionally defensive so logging large or deeply nested
 * objects cannot interrupt request processing.</p>
 */
public final class JsonLogUtil {

    private JsonLogUtil() {
    }

    public static String toJSONString(Object value) {
        try {
            return JSON.toJSONString(value);
        } catch (RuntimeException ex) {
            return "<json serialization failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage() + ">";
        }
    }
}

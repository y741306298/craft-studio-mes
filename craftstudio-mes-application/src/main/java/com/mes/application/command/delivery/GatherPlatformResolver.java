package com.mes.application.command.delivery;

import com.mes.domain.order.orderInfo.vo.OrderChannelInfo;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import java.util.Locale;

/** Routes by gather channel code, never by the ecommerce platformCode. */
public final class GatherPlatformResolver {
    private GatherPlatformResolver() {}

    public static GatherPlatformType resolve(OrderChannelInfo channel) {
        if (channel == null) {
            throw new IllegalArgumentException("订单渠道不能为空");
        }
        return resolveCode(channel.getCode());
    }

    public static GatherPlatformType resolveCode(String code) {
        // Historical orders had no channel code and used WDT.
        String normalized = code == null || code.isBlank() ? "WDT" : code.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "WDT" -> GatherPlatformType.WDT;
            case "JST" -> GatherPlatformType.JST;
            default -> throw new IllegalArgumentException("不支持的聚单平台编码: " + code);
        };
    }
}

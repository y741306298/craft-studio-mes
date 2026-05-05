package com.mes.interfaces.api.config;

import com.alibaba.fastjson.JSON;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class DateTimeZoneConfig {

    private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("Asia/Shanghai");

    static {
        TimeZone.setDefault(DEFAULT_TIME_ZONE);
        JSON.defaultTimeZone = DEFAULT_TIME_ZONE;
        System.setProperty("user.timezone", "Asia/Shanghai");
    }
}

package com.mes.interfaces.api.config;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class DateTimeZoneConfig {

    private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("Asia/Shanghai");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonTimeZoneCustomizer() {
        return builder -> builder
                .timeZone(DEFAULT_TIME_ZONE)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .simpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    static {
        TimeZone.setDefault(DEFAULT_TIME_ZONE);
        JSON.defaultTimeZone = DEFAULT_TIME_ZONE;
    }
}

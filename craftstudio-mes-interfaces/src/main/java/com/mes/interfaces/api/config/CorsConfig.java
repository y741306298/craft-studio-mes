package com.mes.interfaces.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final String ALGORITHM_CORE_API_CALL_RECORD_QUERY_PATH =
            "/api/algorithmCoreApiCallRecord";

    private final ManufacturerSideAuthInterceptor manufacturerSideAuthInterceptor;
    private final ConfigSideAuthInterceptor configSideAuthInterceptor;

    public CorsConfig(ManufacturerSideAuthInterceptor manufacturerSideAuthInterceptor,
                      ConfigSideAuthInterceptor configSideAuthInterceptor) {
        this.manufacturerSideAuthInterceptor = manufacturerSideAuthInterceptor;
        this.configSideAuthInterceptor = configSideAuthInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(manufacturerSideAuthInterceptor)
                .addPathPatterns("/api/manufacturerSide/**")
                .excludePathPatterns(
                        "/api/manufacturerSide/order/add",
                        "/api/manufacturerSide/order/cancel",
                        "/api/manufacturerSide/deliveryPkg/pkgDetail",
                        "/api/manufacturerSide/**/callback/**",
                        "/api/manufacturerSide/deviceCfg/factory/task/claim",
                        "/api/manufacturerSide/deviceCfg/factory/task/download",
                        ALGORITHM_CORE_API_CALL_RECORD_QUERY_PATH
                );

        registry.addInterceptor(configSideAuthInterceptor)
                .addPathPatterns("/api/configSide/**")
                .excludePathPatterns(
                        "/api/configSide/auth/login",
                        "/api/configSide/auth/user/add",
                        "/api/configSide/auth/token/configUserId",
                        ALGORITHM_CORE_API_CALL_RECORD_QUERY_PATH
                );
    }
}

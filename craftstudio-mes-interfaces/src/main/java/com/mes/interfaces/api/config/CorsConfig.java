package com.mes.interfaces.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final ManufacturerSideAuthInterceptor manufacturerSideAuthInterceptor;

    public CorsConfig(ManufacturerSideAuthInterceptor manufacturerSideAuthInterceptor) {
        this.manufacturerSideAuthInterceptor = manufacturerSideAuthInterceptor;
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
                .addPathPatterns("/api/manufacturerSide/**");
    }
}

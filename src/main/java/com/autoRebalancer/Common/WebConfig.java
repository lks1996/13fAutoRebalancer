package com.autoRebalancer.Common;

import com.autoRebalancer.Common.Interceptor.ApiKeyInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${kis.master.api.base-url}")
    private String kisApiBaseUrl;

    @Autowired
    private ApiKeyInterceptor apiKeyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // '/api/**'로 시작하는 모든 주소에 대해 ApiKeyInterceptor 적용.
        registry.addInterceptor(apiKeyInterceptor)
                .addPathPatterns("/api/**");
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(kisApiBaseUrl) // 기본 URL 설정
                // .defaultHeader("X-API-KEY", kisApiKey) // 공통 헤더가 있다면 여기서 설정
                .build();
    }
}

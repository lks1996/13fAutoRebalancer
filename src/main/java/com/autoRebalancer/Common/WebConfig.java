package com.autoRebalancer.Common;

import com.autoRebalancer.Common.Interceptor.ApiKeyInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private ApiKeyInterceptor apiKeyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // '/api/**'로 시작하는 모든 주소에 대해 ApiKeyInterceptor 적용.
        registry.addInterceptor(apiKeyInterceptor)
                .addPathPatterns("/api/**");
    }
}

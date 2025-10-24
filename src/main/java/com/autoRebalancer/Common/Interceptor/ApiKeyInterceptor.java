package com.autoRebalancer.Common.Interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    @Value("${personal-api.secret-key}")
    private String secretKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 요청 헤더에서 'X-API-KEY' 값 가옴.
        String apiKey = request.getHeader("X-API-KEY");

        // 2. 비밀 키가 올바른지 확인.
        if (secretKey.equals(apiKey)) {
            return true; // 올바르다면, 요청 통과.
        }

        // 3. 키가 없거나 틀리면, 접근 거부(401) 응답을 보내고 요청 차단.
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or Missing API Key");
        return false;
    }
}

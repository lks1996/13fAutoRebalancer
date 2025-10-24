package com.autoRebalancer.Kis.Token;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class TokenValidationAspect {
    private final TokenHolder tokenHolder;

    @Before("@annotation(com.autoRebalancer.Kis.Token.RequireValidToken)")
    public void ensureValidToken() {
        tokenHolder.getAccessToken();
    }
}

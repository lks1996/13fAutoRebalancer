package com.autoRebalancer.Common.Mapper;

import com.autoRebalancer.Kis.Dto.TokenRes;
import com.autoRebalancer.Kis.Entity.Token;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

@Component
public class TokenMapper {
    public Token toToken(TokenRes tokenRes, String type) {
        return Token.builder()
                .accessToken(tokenRes.accessToken())
                .createDate(Date.from(Instant.now()))
                .expiration(Date.from(tokenRes.accessTokenTokenExpired().atZone(ZoneId.of("Asia/Seoul")).toInstant()))
                .type(type)
                .build();
    }
}

package com.autoRebalancer.Kis.Token;

import com.autoRebalancer.Kis.Entity.Token;
import com.autoRebalancer.Kis.Repository.TokenRepository;
import com.autoRebalancer.Kis.Service.ConnectionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
public class TokenHolder {

    @Value("${spring.profiles.active}")
    private String profile;
    @Value("${vprofiles}")
    private String vprofile;

    private Token cachedToken;
    private final ConnectionService connectionService;
    private final TokenRepository tokenRepository;

    public TokenHolder(ConnectionService connectionService, TokenRepository tokenRepository) {
        this.connectionService = connectionService;
        this.tokenRepository = tokenRepository;
    }

    /**
     * 토큰 유효성 검사 메서드
     * - 캐시에 저장되어 있는 토큰이 없을 경우, 토큰을 DB에서 조회 후 캐시에 저장.
     * - 토큰이 만료되었거나, 없을 시에 토큰을 발급 받고, 캐시에 저장.
     */
    public synchronized String getAccessToken() {

        // 캐시에 저장되어 있는 토큰이 있고, 유효한 경우.
        if (cachedToken != null && isValid(cachedToken)) {
            log.info("Cached Token is valid.");
            return cachedToken.getAccessToken();
        }
        log.warn("Current profile -->> {}", profile);
        log.warn("Current vprofile -->> {}", vprofile);

        // 캐시에 저장되어 있는 토큰이 없는 경우 DB에서 가장 최근 토큰 조회
        Optional<Token> optionalToken = tokenRepository.findTopByTypeOrderByIdDesc(vprofile);

        // 발급된 토큰이 존재하고, 유효한 경우.
        if ( optionalToken.isPresent() && isValid(optionalToken.get()) ) {
            log.info("Saved Token is valid. Will save this token to cache.");
            cachedToken = optionalToken.get();
            return cachedToken.getAccessToken();
        }

        // 발급된 토큰이 없거나, 만료된 경우.
        try {
            log.info("Token expired or not found, fetching new token...");
            String newToken = connectionService.getNewAccessToken();
            cachedToken = tokenRepository.findTopByTypeOrderByIdDesc(vprofile).orElseThrow();
            return newToken;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void setToken(Token token) {
        this.cachedToken = token;
    }

    // 토큰의 유효성 검사
    private boolean isValid(Token token) {
        return token.getExpiration().toInstant().isAfter(Instant.now());
    }
}

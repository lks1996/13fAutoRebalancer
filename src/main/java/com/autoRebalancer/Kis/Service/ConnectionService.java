package com.autoRebalancer.Kis.Service;

import com.autoRebalancer.Kis.Dto.TokenRes;
import com.autoRebalancer.Kis.Entity.Token;
import com.autoRebalancer.Common.Mapper.TokenMapper;
import com.autoRebalancer.Kis.Repository.TokenRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionService {

    @Value("${vprofiles}")
    private String vprofile;
    @Value("${hantuOpenapi.appkey}")
    private String APP_KEY;
    @Value("${hantuOpenapi.appsecret}")
    private String APP_SECRET;
    @Value("${hantuOpenapi.domain}")
    private String DOMAIN;

    private final ObjectMapper objectMapper;
    private final TokenMapper tokenMapper;
    private final TokenRepository tokenRepository;

    public String getNewAccessToken() throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("grant_type", "client_credentials");
        requestMap.put("appkey", APP_KEY);
        requestMap.put("appsecret", APP_SECRET);

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(requestMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        HttpEntity<String> requestMessage = new HttpEntity<>(jsonBody, httpHeaders);

        // 접근토큰발급
        String URL = DOMAIN + "/oauth2/tokenP";
        ResponseEntity<TokenRes> response = restTemplate.exchange(URL, HttpMethod.POST, requestMessage, TokenRes.class);
        log.info("Response body: {}", response.getBody());

        Token token = tokenMapper.toToken(response.getBody(), vprofile);

        tokenRepository.save(token);

        log.info("Access token obtained: {}", token.getAccessToken());
        log.info("Token expires at: {}", token.getExpiration());

        return token.getAccessToken();
    }
}

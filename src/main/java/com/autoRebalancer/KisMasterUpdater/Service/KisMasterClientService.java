package com.autoRebalancer.KisMasterUpdater.Service;

import com.autoRebalancer.KisMasterUpdater.Dto.StockInfoDto;
import com.autoRebalancer.Common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class KisMasterClientService {
    private final WebClient webClient;

    @Value("${kis.master.api.url}")
    private String kisApiUrl;

    public KisMasterClientService(WebClient webClient) { // 생성자에서 WebClient 받기
        this.webClient = webClient;
    }

    /**
     * KisMasterUpdater API 호출. 종목 마스터 정보 조회.
     * @param symbols 조회할 티커 리스트
     * @return StockMasterDto 리스트
     */
    public List<StockInfoDto> getStockMasterInfo(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 1. API 응답을 Mono<ApiResponse<...>> 형태로 받도록 요청을 정의
            Mono<ApiResponse<List<StockInfoDto>>> responseMono = this.webClient
                    .get() // GET 요청
                    .uri(uriBuilder -> uriBuilder
                            .path(kisApiUrl) // application.yml의 URL 사용
                            .queryParam("symbols", String.join(",", symbols))
                            .build())
                    // .header("X-API-KEY", kisApiKey) // API 키 헤더가 필요하다면 추가
                    .retrieve() // 응답을 가져옴
                    // 응답 본문을 ParameterizedTypeReference를 이용해 파싱
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<StockInfoDto>>>() {});

            // 2. block()을 호출하여 비동기 응답을 동기 방식(결과가 올 때까지 기다림)으로 변환
            ApiResponse<List<StockInfoDto>> response = responseMono.block();

            // 3. 결과 처리
            if (response != null && "SUCCESS".equals(response.status())) {
                return response.data();
            } else {
                log.error("Failed to fetch stock info from KIS Master. Response: {}", response);
                return Collections.emptyList();
            }

        } catch (Exception e) {
            log.error("Error calling KIS Master API with WebClient", e);
            return Collections.emptyList();
        }
    }
}

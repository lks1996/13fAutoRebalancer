package com.autoRebalancer._13f.Service;

import com.autoRebalancer._13f.Dto.Filing;
import com.autoRebalancer._13f.Dto.Holding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DataScrapService {

    private static final String API_BASE_URL = "https://forms13f.com/api/v1";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    /**
     * 최근 공시 정보 조회.
     * @return Filing 리스트
     */
    public List<Filing> getFilings() throws IOException, InterruptedException {
        List<Filing> allFilings = new ArrayList<>();
        int offset = 0;
        final int limit = 250;
        boolean hasMorePages = true;

        // 1. 최근 일주일 날짜 범위 설정.
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusWeeks(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        while (hasMorePages) {
            // 2. 현재 offset과 limit으로 API URL 생성
            String url = String.format("%s/filings?from=%s&to=%s&limit=%d&offset=%d"
                    ,API_BASE_URL
                    , weekAgo.format(formatter)
                    , today.format(formatter)
                    , limit
                    , offset);

            log.info("Fetching filings from page url :: {}", url);

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("Failed to fetch filings page for " + "From : " + weekAgo.format(formatter) + " ~ " + "To : " + today.format(formatter) + ". Offset: " + offset + ". Status code: " + response.statusCode());
            }

            Type filingListType = new TypeToken<List<Filing>>(){}.getType();
            List<Filing> currentPageFilings = gson.fromJson(response.body(), filingListType);

            // 3. API 응답이 비어있는지 확인
            if (currentPageFilings == null || currentPageFilings.isEmpty()) {
                hasMorePages = false;
                log.info("Reached the last page (offset {}).", offset);
            } else {
                // 4. 결과가 있으면 전체 리스트에 추가
                allFilings.addAll(currentPageFilings);
                log.info("Fetched {} filings from offset {}. Total fetched: {}", currentPageFilings.size(), offset, allFilings.size());
                // 5. 다음 페이지를 위해 offset 증가
                offset += limit;
            }
        }

        log.info("Finished fetching all filings. Total {} filings found.", allFilings.size());
        return allFilings;
    }

    /**
     * 특정 공시의 모든 보유 종목 리스트 조회.
     * @param cik 기관의 CIK 번호
     * @param accessionNumber 공시의 고유 번호
     * @return 보유 종목(Holding) 리스트
     */
    public List<Holding> getHoldings(String cik, String accessionNumber) throws IOException, InterruptedException {
        List<Holding> allHoldings = new ArrayList<>(); // 모든 페이지 결과를 합칠 리스트
        int offset = 0; // 시작 페이지 오프셋
        final int limit = 250; // 한 페이지당 가져올 항목 수 (API 기본값)
        boolean hasMorePages = true; // 더 가져올 페이지가 있는지 확인하는 플래그

        log.info("Starting to fetch all holdings for CIK {}, Accession# {}...", cik, accessionNumber);

        while (hasMorePages) {
            // 1. 현재 offset과 limit으로 API URL 생성
            String url = String.format("%s/form?accession_number=%s&cik=%s&limit=%d&offset=%d",
                    API_BASE_URL
                    , accessionNumber
                    , cik
                    , limit
                    , offset);

            log.info("Fetching holdings from page url :: {}", url); // 상세 로그 레벨 변경 (선택)

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("Failed to fetch holdings page for " + accessionNumber + ". Offset: " + offset + ". Status code: " + response.statusCode());
            }

            Type holdingListType = new TypeToken<List<Holding>>(){}.getType();
            List<Holding> currentPageHoldings = gson.fromJson(response.body(), holdingListType);

            // 2. API 응답이 비어있는지 확인
            if (currentPageHoldings == null || currentPageHoldings.isEmpty()) {
                hasMorePages = false; // 빈 응답이면 마지막 페이지이므로 루프 종료
                log.info("Reached the last page (offset {}).", offset);
            } else {
                // 3. 결과가 있으면 전체 리스트에 추가
                allHoldings.addAll(currentPageHoldings);
                log.info("Fetched {} holdings from offset {}. Total fetched: {}", currentPageHoldings.size(), offset, allHoldings.size());
                // 4. 다음 페이지를 위해 offset 증가
                offset += limit;
            }
        }

        log.info("Finished fetching all holdings. Total {} holdings found for CIK {}, Accession# {}.", allHoldings.size(), cik, accessionNumber);
        return allHoldings; // 모든 페이지 결과가 합쳐진 리스트 반환
    }

}

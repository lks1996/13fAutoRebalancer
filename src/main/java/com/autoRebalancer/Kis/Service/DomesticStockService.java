package com.autoRebalancer.Kis.Service;

import com.autoRebalancer.Kis.Dto.StockDto;
import com.autoRebalancer.Kis.Token.RequireValidToken;
import com.autoRebalancer.Kis.Token.TokenHolder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DomesticStockService {

    private final TokenHolder tokenHolder;

    public DomesticStockService(TokenHolder tokenHolder) {

        this.tokenHolder = tokenHolder;
    }

    @Value("${vprofiles}")
    private String vprofile;
    @Value("${hantuOpenapi.appkey}")
    private String APP_KEY;
    @Value("${hantuOpenapi.appsecret}")
    private String APP_SECRET;
    @Value("${hantuOpenapi.domain}")
    private String DOMAIN;
    @Value("${hantuOpenapi.cano}")
    private String CANO;
    @Value("${hantuOpenapi.acntprdtcd}")
    private String ACNT_PRDT_CD;

    // 주식잔고조회_국내주식
    private final String urlBalance = "/uapi/domestic-stock/v1/trading/inquire-balance";
    // 주식현재가 시세_국내주식
    private final String urlInquirePrice = "/uapi/domestic-stock/v1/quotations/inquire-price";
    // 국내주식주문(현금)_국내주식
    private final String urlOrder = "/uapi/domestic-stock/v1/trading/order-cash";
    // 국내주식주문(현금)_국내주식
    private final String urlDailyCcld = "/uapi/domestic-stock/v1/trading/inquire-daily-ccld";

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("authorization", "Bearer " + tokenHolder.getAccessToken());
        headers.set("appkey", APP_KEY);
        headers.set("appsecret", APP_SECRET);

        return headers;
    }

    /**
     * 국내주식잔고조회
     *
     * @return
     */
    @RequireValidToken
    public String getBalance(StockDto stockDto) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();

        if(vprofile.equals("prod")) {
            headers.set("tr_id", "TTTC8434R");  // 실전용
        } else {
            headers.set("tr_id", "VTTC8434R");  // 모의용
        }

        // 주식잔고조회[v1_국내주식-006]
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(DOMAIN + urlBalance)
                .queryParam("CANO", CANO)                                               // 종합계좌번호
                .queryParam("ACNT_PRDT_CD", ACNT_PRDT_CD)                               // 계좌상품코드
                .queryParam("AFHR_FLPR_YN", stockDto.getAfhrFlprYn())                   // 시간외단일가, 거래소여부
                .queryParam("OFL_YN", stockDto.getOflYn())                              // 오프라인여부
                .queryParam("INQR_DVSN", stockDto.getInorDvsn())                        // 조회구분
                .queryParam("UNPR_DVSN", stockDto.getUnprDvsn())                        // 단가구분
                .queryParam("FUND_STTL_ICLD_YN", stockDto.getFundSttlIcldYn())          // 펀드결제분포함여부
                .queryParam("FNCG_AMT_AUTO_RDPT_YN", stockDto.getFncgAmtAutoRdptYn())   // 융자금액자동상환여부
                .queryParam("PRCS_DVSN", stockDto.getPrcsDvsn())                        // 처리구분
                .queryParam("CTX_AREA_FK100", stockDto.getCtxAreaFk100())               // 연속조회검색조건 100
                .queryParam("CTX_AREA_NK100", stockDto.getCtxAreaNk100());              // 연속조회키 100

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);

        log.debug(" response.getBody(): {}",  response.getBody());
        log.info("[DomesticService.getBalance succeed.]");

        // 테스트 호출 시 호출 제한이 있음.
        if(vprofile.equals("dev")){
            try {
                // 2초 대기
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread sleep interrupted", e);
            }
        }

        return response.getBody();
    }

    /**
     * 국내주식현재가 시세
     */
    @RequireValidToken
    public String getDomesticStockPrice(StockDto orderStock) {

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();

        headers.set("custtype", "P");// 고객 타입 (B: 법인 , P: 개인)
        if(vprofile.equals("prod")) {
            headers.set("tr_id", "FHKST01010100");  // 실전용
        } else {
            headers.set("tr_id", "FHKST01010100");  // 모의용
        }

        // 주식현재가 시세[v1_국내주식-008]
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(DOMAIN + urlInquirePrice)
                .queryParam("FID_COND_MRKT_DIV_CODE", orderStock.getFidCondMrktDivCode())   // 조건 시장 분류 코드( J:KRX, NX:NXT, UN:통합 )
                .queryParam("FID_INPUT_ISCD", orderStock.getFidInputIscd());                // 입력 종목코드 (ex 005930 삼성전자)

        HttpEntity<?> entity = new HttpEntity<>(headers);

        log.info("==========================================");
        log.info(" 조건 시장 분류 코드: {}", orderStock.getFidCondMrktDivCode());
        log.info(" 입력 종목코드: {}", orderStock.getFidInputIscd());
        log.info("==========================================");

        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);

        log.debug(" response.getBody(): {}",  response.getBody());
        log.info("[DomesticService.getDomesticStockPrice succeed.]");

        // 테스트 호출 시 호출 제한이 있음.
        if(vprofile.equals("dev")){
            try {
                // 2초 대기
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread sleep interrupted", e);
            }
        }

        return response.getBody();
    }

    /**
     * 국내주식주문(현금)
     */
    @RequireValidToken
    public void orderDomesticStockCash(StockDto orderStock) throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        HttpHeaders headers = getHttpHeaders();

        headers.set("custtype", "P");// 고객 타입 (B: 법인 , P: 개인)
        if(vprofile.equals("prod")) {
            if(orderStock.getOrderType() == 1) {
                headers.set("tr_id", "TTTC0011U");  // 실전용(매도)
            } else if (orderStock.getOrderType() == 2) {
                headers.set("tr_id", "TTTC0012U");  // 실전용(매수)
            }
        } else if(vprofile.equals("dev")){
            if(orderStock.getOrderType() == 1) {
                headers.set("tr_id", "VTTC0011U");  // 모의용(매도)
            } else if (orderStock.getOrderType() == 2) {
                headers.set("tr_id", "VTTC0012U");  // 모의용(매수)
            }
        }

        // 주식주문(현금)[v1_국내주식-001]
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("CANO", CANO);
        requestBody.put("ACNT_PRDT_CD", ACNT_PRDT_CD);
        requestBody.put("PDNO", orderStock.getPdno());          // 종목코드
        requestBody.put("ORD_DVSN", orderStock.getOrdDvsn());   // 주문구분
        requestBody.put("ORD_QTY", orderStock.getOrdQty());     // 주문수량
        requestBody.put("ORD_UNPR", orderStock.getOrdUnpr());   // 주문단가


        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        log.info("==========================================");
        log.info(" 주문타입: {}",  orderStock.getOrderType() == 1 ? "매도":"매수");
        log.info(" 종목코드: {}",  orderStock.getPdno());
        log.info(" 종목이름: {}",  orderStock.getPrdtName());
        log.info(" 주문수량: {}",  orderStock.getOrdQty());
        log.info(" 주문단가: {}",  orderStock.getOrdUnpr());
        log.info("==========================================");

        log.debug("Headers: {}", headers);
        log.debug("Request Body: {}", new ObjectMapper().writeValueAsString(requestBody));

        ResponseEntity<String> response = restTemplate.exchange(
                DOMAIN + urlOrder,
                HttpMethod.POST,
                entity,
                String.class);

        log.debug(" response.getBody(): {}",  response.getBody());
        log.info("[DomesticService.orderDomesticStockCash succeed.]");

        // 테스트 호출 시 호출 제한이 있음.
        if(vprofile.equals("dev")){
            try {
                // 2초 대기
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread sleep interrupted", e);
            }
        }
    }

    /**
     * 주식일별주문체결조회[v1_국내주식-005]
     * 모의투자는 지원하지 않음.
     */
    @RequireValidToken
    public void getDomesticDailyCcld(StockDto stockDto) throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        HttpHeaders headers = getHttpHeaders();

        headers.set("custtype", "P");// 고객 타입 (B: 법인 , P: 개인)
        if(vprofile.equals("prod")) {
            headers.set("tr_id", "TTTC0081R");  // 실전용(3개월 이내 기간)
        } else if(vprofile.equals("dev")){
            headers.set("tr_id", "VTTC0081R");  // 테스트 전용(3개월 이내 기간)
        }

        // 주식일별주문체결조회[v1_국내주식-005]
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(DOMAIN + urlDailyCcld)
                .queryParam("CANO", CANO)
                .queryParam("ACNT_PRDT_CD", ACNT_PRDT_CD)
                .queryParam("INQR_STRT_DT", stockDto.getInqrStrtDt())  // 조회시작일자( YYYYMMDD )
                .queryParam("INQR_END_DT", stockDto.getInqrEndDt()) // 조회종료일자( YYYYMMDD )
                .queryParam("SLL_BUY_DVSN_CD", stockDto.getSllBuyDvsnCd()) // 매도매수구분코드( 00 : 전체 / 01 : 매도 / 02 : 매수 )
                .queryParam("PDNO", stockDto.getPdno()) // 필수값 아님 ! 종목번호 6자리
                .queryParam("ORD_GNO_BRNO", stockDto.getOrdGnoBrno()) // 주문채번지점번호
                .queryParam("ODNO", stockDto.getOdno()) // 필수값 아님 ! 주문번호
                .queryParam("CCLD_DVSN", stockDto.getCcldDvsn()) // 체결구분( '00 전체 / 01 체결 / 02 미체결' )
                .queryParam("INQR_DVSN", stockDto.getInqrDvsn()) // 조회구분( '00 역순 / 01 정순' )
                .queryParam("INQR_DVSN_1", stockDto.getInqrDvsn1()) // 조회구분1( '없음: 전체 / 1: ELW? 2: 프리보드' )
                .queryParam("INQR_DVSN_3", stockDto.getInqrDvsn3()) // 조회구분3( '00 전체 / 01 현금 / 02 신용 / 03 담보 / 04 대주 / 05 대여 / 06 자기융자신규/상환 / 07 유통융자신규/상환' )
                .queryParam("EXCG_ID_DVSN_CD", stockDto.getExcgIdDvsnCd()) // 거래소ID구분코드( 한국거래소 : KRX / 대체거래소 (NXT) : NXT / SOR (Smart Order Routing) : SOR / ALL : 전체 )
                .queryParam("CTX_AREA_FK100", stockDto.getCtxAreaFk100()) // 연속조회검색조건100( '공란 : 최초 조회시 )
                .queryParam("CTX_AREA_NK100", stockDto.getCtxAreaNk100()); // 연속조회키100( '공란 : 최초 조회시 )

        log.info("===================주문체결조회======================");
        log.info(" 주문체결조회 기간: {} - {}",  stockDto.getInqrStrtDt(),  stockDto.getInqrEndDt());
        log.info("==========================================");

        log.debug("Headers: {}", headers);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);

        log.debug(" response.getBody(): {}",  response.getBody());
        log.debug("CANO -> {}", CANO);
        log.debug("ACNT_PRDT_CD -> {}", ACNT_PRDT_CD);
        log.debug("INQR_STRT_DT -> {}", stockDto.getInqrStrtDt());
        log.debug("INQR_END_DT -> {}", stockDto.getInqrEndDt());
        log.debug("SLL_BUY_DVSN_CD -> {}", stockDto.getSllBuyDvsnCd());
        log.debug("ORD_GNO_BRNO -> {}", stockDto.getOrdGnoBrno());
        log.debug("CCLD_DVSN -> {}", stockDto.getCcldDvsn());
        log.debug("INQR_DVSN -> {}", stockDto.getInqrDvsn());
        log.debug("INQR_DVSN_1 -> {}", stockDto.getInqrDvsn1());
        log.debug("INQR_DVSN_3 -> {}", stockDto.getInqrDvsn3());
        log.debug("EXCG_ID_DVSN_CD -> {}", stockDto.getExcgIdDvsnCd());
        log.debug("CTX_AREA_FK100 -> {}", stockDto.getCtxAreaFk100());
        log.debug("CTX_AREA_NK100 -> {}", stockDto.getCtxAreaNk100());
        log.info("[DomesticService.getDomesticDailyCcld succeed.]");

        // 테스트 호출 시 호출 제한이 있음.
        if(vprofile.equals("dev")){
            try {
                // 2초 대기
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread sleep interrupted", e);
            }
        }
    }


    /**
     * 모의투자 전용 거래내역 조회.
     * 웹 직접 호출. OPEN API 아님.
     */
    @RequireValidToken
    public void getTestDomesticOrderHistory(StockDto stockDto) throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        HttpHeaders headers = getHttpHeaders();

        if(vprofile.equals("prod")) {
            log.error("Cannot get Order History because Current VProfile is -> {}", vprofile);
            return;
        }

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("league", "0");
        requestBody.put("userid", CANO);
        requestBody.put("stdate", stockDto.getInqrStrtDt());  // 조회 시작일( YYYYMMDD )
        requestBody.put("eddate", stockDto.getInqrEndDt());   // 조회 종료일( YYYYMMDD )
        requestBody.put("_page", "1");     // 조회 페이지
        requestBody.put("_limit", "10");   // 페이지당 조회 결과 수
        requestBody.put("midpath", "stocks/myasset/trdwon");

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        log.debug("Headers: {}", headers);
        log.debug("Request Body: {}", new ObjectMapper().writeValueAsString(requestBody));

        ResponseEntity<String> response = restTemplate.exchange(
                "https://vts3.koreainvestment.com/apis/htsec/stocks/myasset/trdwon",
                HttpMethod.POST,
                entity,
                String.class);

        log.debug(" response.getBody(): {}",  response.getBody());
        log.info("[DomesticService.getTestDomesticOrderHistory succeed.]");

    }
}

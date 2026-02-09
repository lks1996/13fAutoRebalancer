package com.autoRebalancer.Kis.Service;

import com.autoRebalancer.Kis.Dto.OverseasStockDto;
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
public class OverseasStockService {

    private final TokenHolder tokenHolder;

    public OverseasStockService(TokenHolder tokenHolder) {

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

    // 주식잔고조회_해외주식
    private final String urlBalance = "/uapi/overseas-stock/v1/trading/inquire-balance";
    //
    private final String urlForeignMargin = "/uapi/overseas-stock/v1/trading/foreign-margin";
    // 해외주식 매수가능 금액 조회
    private final String urlPsamount = "/uapi/overseas-stock/v1/trading/inquire-psamount";
    // 해외주식 현재체결가[v1_해외주식-009]
    private final String urlInquirePrice = "/uapi/overseas-price/v1/quotations/price";
    // 해외주식 주문[v1_해외주식-001]
    private final String urlOrder = "/uapi/overseas-stock/v1/trading/order";
    // 해외주식 주문체결내역[v1_해외주식-007]
    private final String urlDailyCcld = "/uapi/overseas-stock/v1/trading/inquire-ccnl";

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
     * 해외주식잔고조회
     * @return
     */
    @RequireValidToken
    public String getBalance(OverseasStockDto stockDto) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();

        if(vprofile.equals("prod")) {
            headers.set("tr_id", "TTTS3012R");  // 실전용
        } else {
            headers.set("tr_id", "VTTS3012R");  // 모의용
        }

        // 해외주식 잔고[v1_해외주식-006]
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(DOMAIN + urlBalance)
                .queryParam("CANO", CANO)                                               // 종합계좌번호
                .queryParam("ACNT_PRDT_CD", ACNT_PRDT_CD)                               // 계좌상품코드
                .queryParam("OVRS_EXCG_CD", stockDto.getOvrsExcgCd())                   // 해외거래소코드
                .queryParam("TR_CRCY_CD", stockDto.getTrCrctCd())                       // 거래통화코드
                .queryParam("CTX_AREA_FK200", stockDto.getCtxAreaFk200())               // 연속조회검색조건 200
                .queryParam("CTX_AREA_NK200", stockDto.getCtxAreaNk200());              // 연속조회키 200

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);

        log.info(" response.getBody(): {}",  response.getBody());
        log.info("[OverseasStockService.getBalance succeed.]");

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
     * 해외증거금 통화별조회
     * (모의투자는 지원하지 않으므로 실전 전용.)
     * @return
     */
    @RequireValidToken
    public String getForeignMargin(OverseasStockDto stockDto) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();

        headers.set("custtype", "P");// 고객 타입 (B: 법인 , P: 개인)
        if(vprofile.equals("prod")) {
            headers.set("tr_id", "TTTC2101R");  // 실전용
        } else {
            headers.set("tr_id", "");  // 모의 지원하지 않음.
            log.error("해외증거금 통화별조회는 모의투자를 지원하지 않음.");
            return null;
        }

        // 해외증거금 통화별조회 [해외주식-035]
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(DOMAIN + urlForeignMargin)
                .queryParam("CANO", CANO)                   // 종합계좌번호
                .queryParam("ACNT_PRDT_CD", ACNT_PRDT_CD);  // 계좌상품코드

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);

        log.info(" response.getBody(): {}",  response.getBody());
        log.info("[OverseasStockService.getForeignMargin succeed.]");

        return response.getBody();
    }

    /**
     * 해외주식 매수가능금액조회 - 특정 종목에 대한 매수가능 금액 조회.
     * (모의투자의 경우 외화 예수금 조회가 불가하여, 대체용도로 사용.)
     * @return
     */
    @RequireValidToken
    public String getPsamount() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();

        if(vprofile.equals("prod")) {
            headers.set("tr_id", "TTTS3007R");  // 실전용
        } else {
            headers.set("tr_id", "VTTS3007R");  // 모의용
        }

        // 해외주식 매수가능금액조회[v1_해외주식-014]
        // 해외예수금 조회를 위한 서비스이므로, 거래소코드, 주문단가, 종목코드는 임의로 고정.
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(DOMAIN + urlPsamount)
                .queryParam("CANO", CANO)                           // 종합계좌번호
                .queryParam("ACNT_PRDT_CD", ACNT_PRDT_CD)           // 계좌상품코드
                .queryParam("OVRS_EXCG_CD", "AMEX")         // 해외거래소코드 ( NASD : 나스닥 / NYSE : 뉴욕 / AMEX : 아멕스 / SEHK : 홍콩 / SHAA : 중국상해 / SZAA : 중국심천 / TKSE : 일본 / HASE : 하노이거래소 / VNSE : 호치민거래소 )
                .queryParam("OVRS_ORD_UNPR", "100000")      // 해외주문단가 (23.8) 정수부분 23자리, 소수부분 8자리
                .queryParam("ITEM_CD", "AAPL");             // 종목코드

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);

        log.info(" response.getBody(): {}",  response.getBody());
        log.info("[OverseasStockService.getPsamount succeed.]");

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
     * 해외주식현재가 시세
     */
    @RequireValidToken
    public String getOverseasStockPrice(OverseasStockDto stockDto) {

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();

        headers.set("tr_id", "HHDFS00000300");  // 실전, 모의용 공통

        // 해외주식 현재체결가[v1_해외주식-009]
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(DOMAIN + urlInquirePrice)
                .queryParam("AUTH", stockDto.getAuth())         // 사용자권한정보
                .queryParam("EXCD", stockDto.getExcd())         // 거래소코드
                .queryParam("SYMB", stockDto.getSymb());        // 종목코드

        HttpEntity<?> entity = new HttpEntity<>(headers);

        log.info("==========================================");
        log.info(" 조건 시장 거래소 코드: {}", stockDto.getExcd());
        log.info(" 입력 종목코드: {}", stockDto.getSymb());
        log.info("==========================================");

        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);

        log.info(" response.getBody(): {}",  response.getBody());
        log.info("[OverseasService.getOverseasStockPrice succeed.]");

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
     * 해외주식주문
     */
    @RequireValidToken
    public String orderOverseasStock(OverseasStockDto orderStock) throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        HttpHeaders headers = getHttpHeaders();

        if(vprofile.equals("prod")) {
            if(orderStock.getOrderType() == 1) {
                headers.set("tr_id", "TTTT1006U");  // 실전용(매도)
            } else if (orderStock.getOrderType() == 2) {
                headers.set("tr_id", "TTTT1002U");  // 실전용(매수)
            }
        } else if(vprofile.equals("dev")){
            if(orderStock.getOrderType() == 1) {
                headers.set("tr_id", "VTTT1001U");  // 모의용(매도)
            } else if (orderStock.getOrderType() == 2) {
                headers.set("tr_id", "VTTT1002U");  // 모의용(매수)
            }
        }

        // 해외주식 주문[v1_해외주식-001]
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("CANO", CANO);
        requestBody.put("ACNT_PRDT_CD", ACNT_PRDT_CD);
        requestBody.put("OVRS_EXCG_CD", orderStock.getOvrsExcgCd());        // 해외거래소코드
        requestBody.put("PDNO", orderStock.getSymb());                      // 상품번호
        requestBody.put("ORD_QTY", orderStock.getOrdQty());                 // 주문수량
        requestBody.put("OVRS_ORD_UNPR", orderStock.getOvrsOrdUnpr());      // 해외주문단가
        requestBody.put("ORD_SVR_DVSN_CD", "0");                            // 주문서버구분코드
        requestBody.put("ORD_DVSN", orderStock.getOrdDvsn());               // 주문구분


        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        log.info("==========================================");
        log.info(" 주문타입: {}",  orderStock.getOrderType() == 1 ? "매도":"매수");
        log.info(" 종목코드: {}",  orderStock.getSymb());
        log.info(" 종목이름: {}",  orderStock.getSymbName());
        log.info(" 주문수량: {}",  orderStock.getOrdQty());
        log.info(" 주문단가: {}",  orderStock.getOvrsOrdUnpr());
        log.info("==========================================");

        log.info("Headers: {}", headers);
        log.info("Request Body: {}", new ObjectMapper().writeValueAsString(requestBody));

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

        ResponseEntity<String> response = restTemplate.exchange(
                DOMAIN + urlOrder,
                HttpMethod.POST,
                entity,
                String.class);

        log.info(" response.getBody(): {}",  response.getBody());
        log.info("[OverSeasService.orderOverseasStock succeed.]");

        return response.getBody();
    }

    /**
     * 해외주식 주문체결내역[v1_해외주식-007]
     */
    @RequireValidToken
    public void getOverseasCcnl(OverseasStockDto stockDto) throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();

        if(vprofile.equals("prod")) {
            headers.set("tr_id", "TTTS3035R");  // 실전용(3개월 이내 기간)
        } else if(vprofile.equals("dev")){
            headers.set("tr_id", "VTTS3035R");  // 테스트 전용(3개월 이내 기간)
        }

        // 해외주식 주문체결내역[v1_해외주식-007]
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(DOMAIN + urlDailyCcld)
                .queryParam("CANO", CANO)
                .queryParam("ACNT_PRDT_CD", ACNT_PRDT_CD)
                .queryParam("PDNO", "%") // 종목번호 6자리 / 전종목일 경우 "%" 입력
                .queryParam("ORD_STRT_DT", stockDto.getOrdStrtDt())  // 조회시작일자( YYYYMMDD )
                .queryParam("ORD_END_DT", stockDto.getOrdEndDt()) // 조회종료일자( YYYYMMDD )
                .queryParam("SLL_BUY_DVSN", stockDto.getSllBuyDvsn()) // 매도매수구분코드( 00 : 전체 / 01 : 매도 / 02 : 매수 )
                .queryParam("CCLD_NCCS_DVSN", stockDto.getCcldNccsDvsn()) // 체결미체결구분 ( 00 : 전체 / 01 : 체결 / 02 : 미체결 )
                .queryParam("OVRS_EXCG_CD", stockDto.getOvrsExcgCd()) // 해외거래소코드
                .queryParam("SORT_SQN", stockDto.getSortSqn()) // 정렬순서
                .queryParam("ORD_DT", stockDto.getOrdDt()) // 주문일자
                .queryParam("ORD_GNO_BRNO", stockDto.getOrdGnoBrno()) // 주문채번지점번호
                .queryParam("ODNO", stockDto.getOrdNo()) // 주문번호
                .queryParam("CTX_AREA_NK200", stockDto.getCtxAreaNk200()) // 연속조회키200
                .queryParam("CTX_AREA_FK200", stockDto.getCtxAreaFk200()); // 연속조회검색조건200

        log.info("===================주문체결조회======================");
        log.info(" 주문체결조회 기간: {} - {}",  stockDto.getOrdStrtDt(),  stockDto.getOrdEndDt());
        log.info("==========================================");

        log.info("Headers: {}", headers);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);

        log.debug(" response.getBody(): {}",  response.getBody());
        log.debug("CANO -> {}", CANO);
        log.debug("ACNT_PRDT_CD -> {}", ACNT_PRDT_CD);
        log.debug("ORD_STRT_DT -> {}", stockDto.getOrdStrtDt());
        log.debug("ORD_END_DT -> {}", stockDto.getOrdEndDt());
        log.debug("SLL_BUY_DVSN_CD -> {}", stockDto.getSllBuyDvsn());
        log.debug("CCLD_NCCS_DVSN -> {}", stockDto.getOrdGnoBrno());
        log.debug("OVRS_EXCG_CD -> {}", stockDto.getOvrsExcgCd());
        log.debug("SORT_SQN -> {}", stockDto.getSortSqn());
        log.debug("ORD_DT -> {}", stockDto.getOrdDt());
        log.debug("ORD_GNO_BRNO -> {}", stockDto.getOrdGnoBrno());
        log.debug("ODNO -> {}", stockDto.getOrdNo());
        log.debug("CTX_AREA_NK200 -> {}", stockDto.getCtxAreaFk200());
        log.debug("CTX_AREA_NK200 -> {}", stockDto.getCtxAreaNk200());

        log.info(" response.getBody(): {}",  response.getBody());
        log.info("[OverseasService.getOverseasCcnl succeed.]");

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
}

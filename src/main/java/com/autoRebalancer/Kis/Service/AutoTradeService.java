package com.autoRebalancer.Kis.Service;

import com.autoRebalancer.Kis.Dto.*;
import com.autoRebalancer.Googlesheet.DTO.SheetDto;
import com.autoRebalancer.Googlesheet.Service.SheetDataImportService;
import com.autoRebalancer.KisMasterUpdater.Dto.StockInfoDto;
import com.autoRebalancer.KisMasterUpdater.Service.KisMasterClientService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.autoRebalancer.Common.Parser;
import com.autoRebalancer.Common.Validator;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AutoTradeService {

    private final SheetDataImportService sheetDataImportService;
    private final OverseasStockService overseasStockService;
    private final KisMasterClientService kisMasterClientService;

    public AutoTradeService(SheetDataImportService sheetDataImportService
            , OverseasStockService overseasStockService
            , KisMasterClientService kisMasterClientService, KisMasterClientService kisMasterClient) {
        this.sheetDataImportService = sheetDataImportService;
        this.overseasStockService = overseasStockService;
        this.kisMasterClientService = kisMasterClientService;
    }

    @Value("${vprofiles}")
    private String vprofile;

    public void execute() throws Exception {

        /**
        * 1. 구글 시트에서 목표 비중 가져오기
        */
        List<List<Object>> sheetDataList = sheetDataImportService.getSheetsData();
        List<SheetDto> sheetList = parseSheetData(sheetDataList);

        /**
        * 2. 잔고 조회
        */
        OverseasStockBalanceResponseDto sbrDto= getCurrentBalance();
        if(!StockBalanceResponseCheck(sbrDto)) return;
        List<OverseasBalanceOutput1Dto> rawHoldingStocks = sbrDto.getOutput1();             // 보유 중인 종목 조회.
        double cashBalance = Parser.safeParseDouble(getCurrentCashBalance().getFrcrOrdPsblAmt1());    // 보유 중인 예수금 조회.

        log.warn("[WARN]실예수금 총액: {}", cashBalance);

        /// FOR TEST ///
        if(vprofile.equals("dev")){
            cashBalance = 10000.0;
            log.warn("[WARN]테스트 예수금 총액: {}", cashBalance);
        }
        /// FOR TEST ///

        // 2-1. 구글시트에 정의되어있는 종목의 티커 Set 생성.
        Set<String> targetTickers = sheetList.stream()
                .map(SheetDto::getStockCode)
                .filter(code -> code != null && !code.isEmpty())
                .collect(Collectors.toSet());

        // 2-2. 현재 보유 종목 티커 Set 생성
        Set<String> holdingTickers = rawHoldingStocks.stream()
                .map(OverseasBalanceOutput1Dto::getOvrsPdno)
                .collect(Collectors.toSet());

        // 2-3. 두 목록을 합쳐 유일한 '전체 티커 리스트' 생성
        Set<String> allTickers = new HashSet<>(targetTickers);
        allTickers.addAll(holdingTickers);

        log.info("[INFO] KIS 마스터 정보 일괄 조회 (전체 대상: {}개)...", allTickers.size());
        Map<String, StockInfoDto> stockInfoMap = getKisMasterInfoMap(new ArrayList<>(allTickers));

        // 2-4 마스터 맵을 주입하여 보유 종목 변환.
        List<OverseasStockDto> holdingStocks = getEnrichedHoldingStocks(rawHoldingStocks, stockInfoMap);

        // 2-5 매도 로직을 위한 포트폴리오 총액 선계산.
        double portfolioTotal = getPortfolioTotal(holdingStocks,  cashBalance);


        /**
        * 3. 매도 필요 종목 추출 및 매도 진행
        */
        // 3-1. 포트폴리오 비중과 비교하여 매도(전량/일부) 필요 리스트 추출
        log.info("[INFO] 매도 필요 종목 계산 시작 (포트폴리오 총액: {})...", portfolioTotal);
        List<OverseasStockDto> toSellList = calculateSellOrders(holdingStocks, sheetList, portfolioTotal, stockInfoMap);

        if (!toSellList.isEmpty()) {
            log.warn("[WARN] 총 {}개의 종목에 대한 매도 주문 시작...", toSellList.size());
            // 3-2. 매도 주문 실행 (orderType=1 (매도))
            List<OverseasStockDto> successfulSellList = orderStocks(toSellList);

            // 3-3. 매도 완료 후 잔고 및 현금 재조회
            log.info("[INFO] 매도 체결 확인 완료. 잔고 및 현금 재조회...");

            sbrDto = getCurrentBalance();
            if(!StockBalanceResponseCheck(sbrDto)) return;
            rawHoldingStocks = sbrDto.getOutput1();
            holdingStocks = getEnrichedHoldingStocks(rawHoldingStocks, stockInfoMap);

            if(vprofile.equals("prod")) {
                cashBalance = Parser.safeParseDouble(getCurrentCashBalance().getFrcrOrdPsblAmt1());
                log.warn("[WARN] 매도 완료 후 실제 예수금: {}", cashBalance);
            } else {
                cashBalance = computeEstimateRestCashBalance(cashBalance, successfulSellList);
                log.warn("[WARN] 매도 완료 후 테스트 예수금: {}", cashBalance);
            }

            // 3-4. 매도 체결 대기
            // 중요: 'holdingStocks'는 매도 주문 '전'의 상태.
            boolean sellCompleted = waitForTradeCompletion(successfulSellList, holdingStocks, 5, 5000);

            if (!sellCompleted) {
                log.warn("[WARN] 매도 후 예상 예수금 총액: {}", cashBalance);
            }

        } else {
            log.info("[INFO] 매도 필요 종목이 없습니다.");
        }

        /**
        * 4. 보유하고 있지 않은 종목이 있다면, 해당 종목을 먼저 구매함.(단, 보유 중인 종목은 지정된 비율만큼 이미 보유하고 있다고 가정.)
        */
        // 4-1. 미보유 종목 추출.( 미보유 종목이더라도 목표비중이 0이라면 제외함. )
        List<OverseasStockDto> tempHoldingStocks = holdingStocks;
        List<SheetDto> unholdingStockList = sheetList.stream()
                .filter(sheet -> sheet.getTargetRatio() > 0)
                .filter(sheet -> tempHoldingStocks.stream()
                        .noneMatch(own -> own.getSymb().equals(sheet.getStockCode())))
                .toList();

        // 미보유 종목이 존재한다면,
        if (!unholdingStockList.isEmpty()) {
            // 4-1. 전체 금액 계산.
            // 포트폴리오 총액 = 보유 종목 평가금액 합 + 예수금
            portfolioTotal = getPortfolioTotal(holdingStocks,  cashBalance);

            // 4-2. 미보유 종목 매수 필요 리스트 추출.
            List<OverseasStockDto> toBuyList = calculateUnholdingBuys(unholdingStockList, portfolioTotal, cashBalance, stockInfoMap);

            if (!toBuyList.isEmpty()) {

                // 4-3. 추가 매수가 필요한 종목 주문.
                List<OverseasStockDto> successfulBuyList = orderStocks(toBuyList);

                // 4-4. 잔존 예상 예수금 계산.
                cashBalance = computeEstimateRestCashBalance(cashBalance, successfulBuyList);
                log.warn("[WARN]미보유 종목 매수 후 예상 예수금 총액: {}", cashBalance);

                // 4-5. 미보유 종목이 매수되었는지 확인.
                // 중요: 'holdingStocks'는 매도 '후' & 미보유 매수 '전'의 상태.
                boolean buyCompleted = waitForTradeCompletion(successfulBuyList, holdingStocks, 5, 5000);

                if ( !buyCompleted ) {
                    log.warn("[WARN]미보유 종목 매수 미체결 상태로 리밸런싱 시작.");
                }
            }
        }

        /**
        * 5. 이후에 보유 종목에 대해 포트폴리오 비율과 비교하여 추가 매수 진행.(보유 종목의 현재 비율은 내림 처리.)
        */
        // 5-1. 잔고 재조회.
        sbrDto = getCurrentBalance();
        if(!StockBalanceResponseCheck(sbrDto)) return;

        rawHoldingStocks = sbrDto.getOutput1();     // 미보유 매수 후 현재 보유 중인 종목 재확인.
        holdingStocks = getEnrichedHoldingStocks(rawHoldingStocks, stockInfoMap);

        if(vprofile.equals("prod")) {
            cashBalance = Parser.safeParseDouble(getCurrentCashBalance().getFrcrOrdPsblAmt1());     // 미보유 매수 후 남은 실제 예수금 확인.
        }

        // 5-2. 추가 매수 필요 리스트 추출.
        List<OverseasStockDto> rebalanceBuyList = calculateRebalanceBuys(holdingStocks, sheetList, cashBalance);

        if (!rebalanceBuyList.isEmpty()) {
            // 5-3. 추가 매수가 필요한 종목 주문.
            List<OverseasStockDto> successfulRebalanceList = orderStocks(rebalanceBuyList);
            // 5-4. 잔존 예상 예수금 계산.
            cashBalance = computeEstimateRestCashBalance(cashBalance, successfulRebalanceList);
        }

        log.warn("[WARN]리밸런싱 종목 매수 후 예상 예수금 총액: {}", cashBalance);

        log.info("==========================");
        log.warn("[WARN]자동 매수 처리 완료.");
        log.info("==========================");
    }

    /**
     * 포트폴리오 데이터 임포트.
     * @param sheetDataList 구글 시트 전체 데이터 목록
     * @return resultList 포트폴리오 목록
     * @throws Exception
     */
    private List<SheetDto> parseSheetData(List<List<Object>> sheetDataList) throws Exception {

        List<SheetDto> resultList = new ArrayList<>();

        for (int i = 0; i < sheetDataList.size(); i++) { // i=1부터 시작: 첫 줄은 헤더
            List<Object> row = sheetDataList.get(i);

            String stockCode = row.size() > 0 ? row.get(0).toString().trim() : "";
            String stockName = row.size() > 1 ? row.get(1).toString().trim() : "";
            long sshPrnamt = row.size() > 2 ? Parser.safeParseLong(row.get(2)) : 0;
            double value = row.size() > 3 ? Parser.safeParseDouble(row.get(3)) : 0;;
            double targetRatio = row.size() > 4 ? Parser.safeParseDouble(row.get(4)) : 0.0;

            resultList.add(new SheetDto("", stockCode, stockName, sshPrnamt, value, targetRatio));
        }
        return resultList;
    }

    /**
     * 해외주식 잔고 조회(보유종목)
     * @return OverseasStockBalanceResponseDto
     */
    private OverseasStockBalanceResponseDto getCurrentBalance() throws Exception{
        OverseasStockDto requestDto = new OverseasStockDto();
        String balancerResponse = overseasStockService.getBalance(requestDto);

        ObjectMapper mapper = new ObjectMapper();
        OverseasStockBalanceResponseDto resultDto = mapper.readValue(balancerResponse, OverseasStockBalanceResponseDto.class);

        return resultDto;
    }

    /**
     * 해외주식 매수가능금액 조회.
     * @return OverseasStockBalanceResponseDto
     */
    private OverseasPsblAmountOutputDto getCurrentCashBalance() throws Exception{
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(overseasStockService.getPsamount());
        JsonNode outputNode = rootNode.path("output");
        OverseasPsblAmountOutputDto resultDto = mapper.treeToValue(outputNode, OverseasPsblAmountOutputDto.class);

        return resultDto;
    }

    /**
     * 잔고 응답 유효성 체크
     * @param sbrDto
     * @return boolean
     */
    private boolean StockBalanceResponseCheck(OverseasStockBalanceResponseDto sbrDto){
        if( sbrDto.getOutput1()==null || sbrDto.getOutput2()==null ){
            log.error("[ERROR] 잔고 조회 실패. output1 혹은 output2가 null.");
            return false;
        } else if ( sbrDto.getOutput1().isEmpty() ) {
            log.error("[ERROR] 잔고 조회 실패. output1가 비어있음.");
            return false;
        }
        return true;
    }


    /**
     * 현재가 조회.
     * @param stockCode 종목코드
     * @return resultDto 현재 종목 정보
     * @throws Exception
     */
    private StockPriceResponseDto getCurrentStockPrice(String stockCode, String ovrsExcgCd) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        // 현재가 조회를 위한 세팅.
        OverseasStockDto stockPriceDto = new OverseasStockDto();
        stockPriceDto.setSymb(stockCode);
        stockPriceDto.setExcd(ovrsExcgCd);

        // 현재가 조회 결과를 StockPriceResponseDto에 매핑.
        JsonNode rootNode = mapper.readTree(overseasStockService.getOverseasStockPrice(stockPriceDto));
        JsonNode outputNode = rootNode.path("output");
        StockPriceResponseDto resultDto = mapper.treeToValue(outputNode, StockPriceResponseDto.class);

        return resultDto;
    }

    /**
     * 보유 종목과 현금 예수금으로 총 보유 금액을 계산.
     * @param holdingStocks 총 보유 종목
     * @param cashBalance 현금 예수금
     * @return result
     */
    private double getPortfolioTotal(List<OverseasStockDto> holdingStocks, double cashBalance) {
        double holdingStockValue = holdingStocks.stream()
                .mapToDouble(h -> Parser.safeParseDouble(h.getEvluAmt()))
                .sum();
        log.info("[INFO] 총 보유주식 평가금액: {}, 현금 예수금: {}, 총 포트폴리오 가치: {}", holdingStockValue, cashBalance, holdingStockValue + cashBalance);
        return holdingStockValue + cashBalance;
    }

    /**
     * 미보유 종목 매수 계산.
     * @param unholdingStockList 미보유 종목 목록
     * @param portfolioTotal 총 보유 종목 평가금 + 현금 예수금
     * @return resultList 매수 대상 종목 리스트
     * @throws Exception
     */
    private List<OverseasStockDto> calculateUnholdingBuys(List<SheetDto> unholdingStockList, double portfolioTotal, double cashBalance, Map<String, StockInfoDto> stockInfoMap) throws Exception {

        List<OverseasStockDto> resultList = new ArrayList<>();
        List<Map<String, Object>> resultLogList = new ArrayList<>();

        for (SheetDto p : unholdingStockList) {
            String symbol = p.getStockCode();
            // 티커에 맞는 종목 마스터 파일 정보 세팅.
            StockInfoDto stockInfo = stockInfoMap.get(symbol);

            // 목표 금액 = 포트폴리오 총액 * 목표 비중
            double targetAmount = portfolioTotal * (p.getTargetRatio() / 100.0);

            // 미보유 종목이므로 현재 보유금액은 0
            double currentHoldingAmount = 0.0;

            // 부족분 = 목표 금액 - 현재 보유 금액
            double needToBuyAmount = targetAmount - currentHoldingAmount;

            if (needToBuyAmount > cashBalance) {
                needToBuyAmount = cashBalance;
            }

            // 현재가 조회
            StockPriceResponseDto sprDto = getCurrentStockPrice(symbol, stockInfo.exchangeId());
            double stockPrice = 0.0;
            try {
                stockPrice = Double.parseDouble(sprDto.getLast());
            } catch (NumberFormatException e) {
                log.error("[ERROR] 현재가 파싱 실패: {} (종목: {})", sprDto.getLast(), p.getStockCode());
                continue; // 가격 파싱 실패 시 이 종목은 건너뜁니다.
            } catch (Exception e) {
                log.error("[ERROR] 현재가 조회 중 알 수 없는 오류: (종목: {})", p.getStockCode(), e);
                continue;
            }

            long quantityToBuy = 0;
            if (stockPrice > 0 && needToBuyAmount >= stockPrice) {
                quantityToBuy = (long) Math.floor(needToBuyAmount / stockPrice);
            }

            // 구매 필요 수량이 있는 경우 리스트에 추가
            if (quantityToBuy > 0) {
                resultList.add(OverseasStockDto.builder()
                        .ovrsExcgCd(stockInfo.exchangeId())
                        .symb(symbol)
                        .symbName(p.getStockName())
                        .ovrsOrdUnpr(String.valueOf(stockPrice))
                        .ordQty(String.valueOf(quantityToBuy))
                        .ordDvsn("00")
                        .orderType(2)
                        .build()
                );
            }

            // 결과 저장용 로그
            Map<String, Object> resultLogMap = new HashMap<>();
            resultLogMap.put("code", p.getStockCode());
            resultLogMap.put("name", p.getStockName());
            resultLogMap.put("needToBuyAmount", needToBuyAmount);
            resultLogMap.put("price", stockPrice);
            resultLogMap.put("qty", quantityToBuy);
            resultLogList.add(resultLogMap);
        }

        log.info("===미보유 종목 존재===");

        // 결과 출력 ( 종목명, 종목코드, 구매금액, 실제 구매비율 )
        double sumNeedToBuyAmount = resultLogList.stream()
                .mapToDouble(m -> (double) m.get("needToBuyAmount"))
                .sum();

        for (Map<String, Object> res : resultLogList) {
            double buyRatio = sumNeedToBuyAmount > 0
                    ? ((double) res.get("needToBuyAmount") * 100.0) / sumNeedToBuyAmount
                    : 0.0;
            res.put("buyRatio", buyRatio);

            log.info("미보유 종목 {}({}): 구매금액 ${}, 실제 구매비율 {}%",
                    res.get("name"),
                    res.get("code"),
                    res.get("needToBuyAmount"),
                    buyRatio
            );
        }

        return resultList;
    }

    /**
     * 미보유 종목 매수/매도 체결 확인.
     * @param orderList             주문 리스트
     * @param originalHoldingStocks 주문 '전'의 보유 종목 리스트
     * @param maxRetries            재시도 횟수
     * @param intervalMillis        재시도 간격
     * @return boolean
     */
    public boolean waitForTradeCompletion(List<OverseasStockDto> orderList, List<OverseasStockDto> originalHoldingStocks, int maxRetries, long intervalMillis) {

        if (orderList == null || orderList.isEmpty()) {
            log.warn("[INFO] 체결 확인할 주문 내역이 없습니다.");
            return true;
        }

        // 1. 추적을 위해 수정 가능한 리스트(Pending List)로 복사
        List<OverseasStockDto> pendingOrders = new ArrayList<>(orderList);

        // 주문 타입 확인 (1:매도, 2:매수)
        int orderType = orderList.get(0).getOrderType();
        String tradeAction = (orderType == 1) ? "매도" : "매수";

        // 주문 '전' 상태를 맵으로 변환 (조회용)
        Map<String, Long> originalQtyMap = originalHoldingStocks.stream()
                .collect(Collectors.toMap(
                        OverseasStockDto::getSymb,
                        h -> Parser.safeParseLong(h.getOrdQty()),
                        (existing, replacement) -> existing // 혹시 모를 중복 티커 처리
                ));

        log.warn("[INFO] {} 체결 대기 시작... (대상 {}건)", tradeAction, orderList.size());

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // 2. 현재 잔고 조회.
                OverseasStockBalanceResponseDto sbrDto = getCurrentBalance();
                if (!StockBalanceResponseCheck(sbrDto)) {
                    log.error("[ERROR] {} 체결 확인 중 잔고 조회 실패 (output null). (시도 {}/{})", tradeAction, attempt, maxRetries);
                    Thread.sleep(intervalMillis);
                    continue;
                }

                // 3. 현재 보유 수량을 맵으로 변환. (ordPsblQty: 주문 가능 수량 기준)
                Map<String, Long> currentQtyMap = sbrDto.getOutput1().stream()
                        .collect(Collectors.toMap(
                                OverseasBalanceOutput1Dto::getOvrsPdno,
                                h -> Parser.safeParseLong(h.getOrdPsblQty()),
                                (existing, replacement) -> existing
                        ));

                // 4. 모든 주문이 체결되었는지 확인.
                Iterator<OverseasStockDto> iterator = pendingOrders.iterator();
                while (iterator.hasNext()) {
                    OverseasStockDto order = iterator.next();
                    String symbol = order.getSymb();
                    long orderQty = Parser.safeParseLong(order.getOrdQty());
                    long originalQty = originalQtyMap.getOrDefault(symbol, 0L);
                    long currentQty = currentQtyMap.getOrDefault(symbol, 0L);

                    boolean isCompleted = false;

                    if (orderType == 1) { // [매도 확인]
                        long expectedQtyAfterSell = originalQty - orderQty;
                        if (expectedQtyAfterSell < 0) expectedQtyAfterSell = 0;
                        // 매도는 정확히 수량이 줄어들어야 함
                        if (currentQty == expectedQtyAfterSell) {
                            isCompleted = true;
                        }
                    } else { // [매수 확인] (orderType == 2)
                        long expectedQtyAfterBuy = originalQty + orderQty;
                        // 매수는 수량이 늘어나야 함 (>= 사용)
                        if (currentQty >= expectedQtyAfterBuy) {
                            isCompleted = true;
                        }
                    }

                    // 체결 완료 확인.
                    if (isCompleted) {
                        log.info("[CHECK] {} 체결 확인 완료: {} ({}주)", tradeAction, order.getSymbName(), orderQty);
                        iterator.remove(); // pendingOrders 리스트에서 제거
                    }

                    // 5. 모든 주문이 처리되었는지 확인.
                    if (pendingOrders.isEmpty()) {
                        log.warn("[INFO] 모든 {} 주문 체결 완료 확인됨. (총 {}건)", tradeAction, orderList.size());
                        return true;
                    }

                    log.info("[INFO] {} 체결 대기 중... (남은 주문: {}건 / 시도 {}/{})", tradeAction, pendingOrders.size(), attempt, maxRetries);
                    Thread.sleep(intervalMillis);
                }

            } catch (Exception e) {
                log.error("[ERROR] {} 체결 확인 중 예외 발생", tradeAction, e);
                try {
                    Thread.sleep(intervalMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        // 6. 최대 횟수 초과 시 실패한 종목들 출력
        String failedStocks = pendingOrders.stream()
                .map(o -> o.getSymbName() + "(" + o.getSymb() + ")")
                .collect(Collectors.joining(", "));

        log.error("[ERROR] 지정된 시간 내 {} 체결 확인 실패. 미체결 {}건: [{}]",
                tradeAction, pendingOrders.size(), failedStocks);

        return false;
    }

    /**
     * 보유 종목 중 매도 필요 종목 계산 (전량 매도 / 부분 매도).
     * @param holdingStocks 현재 보유 종목 목록
     * @param sheetList 포트폴리오 목표 비중 목록
     * @param portfolioTotal 총 포트폴리오 가치
     * @param stockInfoMap 마스터 정보 맵
     * @return resultList 매도 대상 종목 리스트
     * @throws Exception
     */
    private List<OverseasStockDto> calculateSellOrders(List<OverseasStockDto> holdingStocks, List<SheetDto> sheetList, double portfolioTotal, Map<String, StockInfoDto> stockInfoMap) throws Exception {

        List<OverseasStockDto> resultList = new ArrayList<>();

        // 목표 비중이 0보다 큰 '유효한' 티커 목록
        Set<String> targetTickers = sheetList.stream()
                .filter(s -> s.getTargetRatio() > 0)
                .map(SheetDto::getStockCode)
                .collect(Collectors.toSet());

        for (OverseasStockDto holding : holdingStocks) {
            String stockCode = holding.getSymb();
            double currentAmount = Parser.safeParseDouble(holding.getEvluAmt());
            long currentQty = Parser.safeParseLong(holding.getOrdQty()); // 현재 매도 가능(보유) 수량

            if (currentAmount <= 0.0 || currentQty <= 0) continue; // 매도할 수량이 없음

            StockInfoDto stockInfo = stockInfoMap.get(stockCode);
            if (stockInfo == null) {
                log.error("[ERROR] 매도 계산 중 {}의 마스터 정보 조회 실패.", stockCode);
                continue;
            }

            // 현재가 조회 (매도 주문 시 지정가로 사용하기 위해)
            StockPriceResponseDto sprDto = getCurrentStockPrice(stockCode, stockInfo.exchangeId());
            double stockPrice = Parser.safeParseDouble(sprDto.getLast());

            if (stockPrice <= 0) {
                log.error("[ERROR] 매도 계산 중 {}의 현재가 조회 실패. (가격: {})", stockCode, sprDto.getLast());
                continue;
            }

            // 1. 목표 비중 리스트에 없는(혹은 비중 0) 종목 (전량 매도)
            if (!targetTickers.contains(stockCode)) {
                log.warn("[WARN] 포트폴리오 이탈 종목(전량 매도): {} ({}주)", holding.getSymbName(), currentQty);
                resultList.add(
                        OverseasStockDto.builder()
                                .ovrsExcgCd(holding.getOvrsExcgCd())
                                .symb(stockCode)
                                .symbName(holding.getSymbName())
                                .ovrsOrdUnpr(String.valueOf(sprDto.getLast()))
                                .ordQty(String.valueOf(currentQty))
                                .ordDvsn("00")
                                .orderType(1)
                                .build()
                );
                continue;
            }

            // 2. 목표 비중이 있으나, 현재 비중이 목표 비중보다 높은 종목 (부분 매도)
            double targetRatio = sheetList.stream()
                    .filter(s -> s.getStockCode().equals(stockCode))
                    .mapToDouble(SheetDto::getTargetRatio)
                    .findFirst()
                    .orElse(0.0);

            double targetAmount = portfolioTotal * (targetRatio / 100.0);

            // 현재 평가금액이 목표 금액보다 클 경우 (초과 보유)
            if (currentAmount > targetAmount) {
                double amountToSell = currentAmount - targetAmount; // 매도 필요 금액

                // 매도 수량 계산 (소수점 버림)
                long quantityToSell = (long) Math.floor(amountToSell / stockPrice);

                // 매도할 수량이 0보다 크고, 보유 수량보다 적거나 같아야 함
                if (quantityToSell > 0) {
                    if (quantityToSell > currentQty) {
                        quantityToSell = currentQty; // 계산상 오류로 보유 수량을 초과할 경우, 보유 수량만큼만 매도
                        log.warn("[WARN] 계산된 매도 수량이 보유 수량을 초과. 보유 수량만큼(전량) 매도: {} ({}주)", stockCode, quantityToSell);
                    }

                    log.info("[INFO] 보유 종목 비율 조정 매도: {} ({}) ({}주, 약 {}원)", stockCode, holding.getSymbName(), quantityToSell, amountToSell);

                    resultList.add(
                            OverseasStockDto.builder()
                                    .ovrsExcgCd(holding.getOvrsExcgCd())
                                    .symb(stockCode)
                                    .symbName(holding.getSymbName())
                                    .ovrsOrdUnpr(String.valueOf(sprDto.getLast()))
                                    .ordQty(String.valueOf(quantityToSell))
                                    .ordDvsn("00")
                                    .orderType(1)
                                    .build()
                    );

                }
            }
        }
        return resultList;
    }

    /**
     * 보유 종목 중 추가 매수 필요 종목 계산.
     * @param holdingStocks 현재 보유 종목 목록
     * @param sheetList 포트폴리오 목표 비중 목록
     * @param cashBalance 현재 예수금
     * @return resultList 매수 대상 종목 리스트
     * @throws Exception
     */
    private List<OverseasStockDto> calculateRebalanceBuys(List<OverseasStockDto> holdingStocks, List<SheetDto> sheetList, double cashBalance) throws Exception {
        // 1. 매도/미보유 매수 후 갱신된 잔고기준 총 평가 금액 재계산. (보유 종목 평가금 + 예수금)
        double totalEvalAmount = getPortfolioTotal(holdingStocks, cashBalance);

        // 2. 비율 비교 후 부족분 매수.
        List<OverseasStockDto> resultList = new ArrayList<>();

        if (holdingStocks.isEmpty()) { // 보유 종목 없으면 스킵
            log.info("[INFO] 리밸런싱 매수 대상 보유 종목 없음.");
            return resultList;
        }

        for (OverseasStockDto holding : holdingStocks) {
            if (cashBalance <= 0) {
                log.info("[INFO]예수금 소진으로 추가 매수 중단");
                break;
            }

            String stockCode = holding.getSymb();
            double evalAmt = Parser.safeParseDouble(holding.getEvluAmt());

            // 포트폴리오 목표 비율 추출.
            double targetRatio = sheetList.stream()
                    .filter(s -> s.getStockCode().equals(stockCode))
                    .mapToDouble(SheetDto::getTargetRatio)
                    .findFirst()
                    .orElse(0.0);

            if ( targetRatio <= 0 || evalAmt <= 0 ) continue; // 평가금액 0 이하면 계산 무시

            // 현재 비율 계산.
            double currentRatio = (evalAmt / totalEvalAmount) * 100.0;

            // 목표 비중 보다 낮을 때만 매수 진행.
            if (currentRatio < targetRatio) {
                double shortageRatio = targetRatio - currentRatio;                              // 목표비중 - 현재비중
                double shortageAmount = totalEvalAmount * (shortageRatio / 100.0);    // 목표 비중에 도달하기 위해 필요한 추가 매수 금액.

                // 잔액 초과 방지.( 추가 매수 필요 금액보다 예수금이 적은 경우 예수금에 맞게 매수되도록 금액 조정. )
                if (shortageAmount > cashBalance) {
                    shortageAmount = cashBalance;
                }

                // 현재가 조회.
                StockPriceResponseDto sprDto = getCurrentStockPrice(stockCode, holding.getOvrsExcgCd());
                double stockPrice = Parser.safeParseDouble(sprDto.getLast());

                // 수량 계산. (0주 허용)
                long quantityToBuy = (long) Math.floor(shortageAmount / stockPrice);

                // 수량이 0 초과인 경우에만 매수 리스트에 추가.
                if (quantityToBuy > 0) {
                    resultList.add(OverseasStockDto.builder()
                            .ovrsExcgCd(holding.getOvrsExcgCd())
                            .symb(stockCode)
                            .symbName(holding.getSymbName())
                            .ovrsOrdUnpr(String.valueOf(stockPrice))
                            .ordQty(String.valueOf(quantityToBuy))
                            .ordDvsn("00")
                            .orderType(2)
                            .build()
                    );

                    double buyCost = quantityToBuy * stockPrice;
                    cashBalance -= buyCost;

                    log.info("[INFO] 보유 종목 비율 조정 매수: {} ({}) ({}주, {}원), 남은 예수금: {}원",
                            stockCode, holding.getSymbName(), quantityToBuy, buyCost, cashBalance);
                }
            }
        }
        return resultList;
    }

    /**
     * 매수/매도 주문 실행
     * OverseasStockDto의 orderType (1:매도, 2:매수)에 따라 주문 서비스 호출.
     */
    private List<OverseasStockDto> orderStocks(List<OverseasStockDto> orders) throws Exception {
        List<OverseasStockDto> successfulOrders = new ArrayList<>(); // 성공한 주문을 담을 리스트
        ObjectMapper mapper = new ObjectMapper();

        for (OverseasStockDto order : orders) {
            String orderAction = (order.getOrderType() == 1) ? "매도" : "매수";

            // 1. 유효성 검사.
            if (!Validator.isValidOverseasOrder(order)) {
                log.warn("[WARN] 유효하지 않은 {} 주문(SKIP): Symbol={}, Qty={}", orderAction, order.getSymb(), order.getOrdQty());
                continue;
            }

            try {
                log.warn("[INFO] {} 주문 전송 시도: {} ({}주, 가격: {})", orderAction, order.getSymbName(), order.getOrdQty(), order.getOvrsOrdUnpr());

                // 2. API 호출.
                String jsonResponse = overseasStockService.orderOverseasStock(order);

                // 3. 응답 파싱.
                OrderResponseDto responseDto = mapper.readValue(jsonResponse, OrderResponseDto.class);

                // 4. 결과 확인. (rt_cd: 0 성공, 그 외 실패)
                if ("0".equals(responseDto.getRtCd())) {
                    log.warn("[SUCCESS] {} 주문 접수 성공: {}", orderAction, order.getSymbName());
                    successfulOrders.add(order); // 성공 리스트에 추가

                } else {
                    // 실패 시
                    log.error("[FAIL] {} 주문 접수 실패: {} - 사유: {} (Code: {})",
                            orderAction, order.getSymbName(), responseDto.getMsg1(), responseDto.getRtCd());
                }

            } catch (Exception e) {
                log.error("[ERROR] {} 주문 처리 중 시스템 예외 발생 {}: {}", orderAction, order.getSymb(), e.getMessage());
            }
        }
        // 5. 성공한 주문 목록만 반환.
        return successfulOrders;
    }

    /**
     * 주문 체결 후 예상 예수금 계산.
     */
    private double computeEstimateRestCashBalance(double cashBalance, List<OverseasStockDto> orders) throws Exception {
        if (orders == null || orders.isEmpty()) {
            return cashBalance;
        }
        double totalOrderCost = orders.stream().mapToDouble(dto -> {
            try {
                double price = Double.parseDouble(dto.getOvrsOrdUnpr());
                long qty = Long.parseLong(dto.getOrdQty());
                return price * qty;
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }).sum();

        int orderType = orders.get(0).getOrderType();
        if (orderType == 1) { // 1: 매도
            return cashBalance + totalOrderCost;
        } else if (orderType == 2) { // 2: 매수
            return cashBalance - totalOrderCost;
        }

        return cashBalance;
    }

    /**
     * 티커(Symbol) 문자열 리스트를 받아 KIS 마스터 정보를 조회.
     * 티커를 Key, StockInfoDto 객체를 Value로 하는 Map 반환.
     */
    private Map<String, StockInfoDto> getKisMasterInfoMap(List<String> tickers) {
        if (tickers == null || tickers.isEmpty() || tickers.stream().allMatch(String::isEmpty)) {
            log.warn("[WARN] KIS 마스터 정보 조회할 티커 목록이 비어있습니다.");
            return Collections.emptyMap(); // 빈 리스트면 빈 맵 반환
        }

        // 1. 중복 제거
        List<String> distinctTickers = tickers.stream().distinct().collect(Collectors.toList());

        try {
            // 2. 거래소 정보 일괄 조회
            List<StockInfoDto> stockInfos = kisMasterClientService.getStockMasterInfo(distinctTickers);

            // 3. 티커-StockInfoDto 맵 생성 및 반환
            return stockInfos.stream()
                    .filter(info -> info.symbol() != null && !info.symbol().isEmpty())
                    .collect(Collectors.toMap(
                            StockInfoDto::symbol,
                            info -> info,
                            (existing, replacement) -> existing
                    ));

        } catch (Exception e) {
            log.error("Error fetching KIS master info", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 주어진 DTO 리스트에서 티커 추출, KIS 마스터 정보를 조회.
     * @param balanceList KIS 잔고 API 원본 응답 (output1)
     * @return 마스터 정보가 모두 포함된 OverseasStockDto 리스트
     */
    private List<OverseasStockDto> getEnrichedHoldingStocks(List<OverseasBalanceOutput1Dto> balanceList, Map<String, StockInfoDto> stockInfoMap) {
        if (balanceList == null || balanceList.isEmpty()) {
            return new ArrayList<>();
        }

        List<OverseasStockDto> enrichedList = new ArrayList<>();
        for (OverseasBalanceOutput1Dto balance : balanceList) {
            StockInfoDto info = stockInfoMap.get(balance.getOvrsPdno()); // API 호출 대신 Map에서 조회

            String exchangeId = (info != null) ? info.exchangeId() : "";
            String stockName = (info != null && info.koreanName() != null && !info.koreanName().isEmpty())
                    ? info.koreanName() : balance.getOvrsItemName();
            String currency = (info != null) ? info.currency() : "";

            enrichedList.add(OverseasStockDto.builder()
                    .symb(balance.getOvrsPdno())
                    .symbName(stockName)
                    .ovrsExcgCd(exchangeId)
                    .trCrctCd(currency)
                    .ordQty(balance.getOrdPsblQty())    // 현재 매도 가능 수량
                    .evluAmt(balance.getOvrsStckEvluAmt())
                    .build());
        }
        return enrichedList;
    }
}
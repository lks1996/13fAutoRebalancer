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

    private final DomesticStockService domesticStockService;
    private final SheetDataImportService sheetDataImportService;
    private final OverseasStockService overseasStockService;
    private final KisMasterClientService kisMasterClientService;

    public AutoTradeService(DomesticStockService domesticStockService
            , SheetDataImportService sheetDataImportService
            , OverseasStockService overseasStockService
            , KisMasterClientService kisMasterClientService, KisMasterClientService kisMasterClient) {
        this.domesticStockService = domesticStockService;
        this.sheetDataImportService = sheetDataImportService;
        this.overseasStockService = overseasStockService;
        this.kisMasterClientService = kisMasterClientService;
    }

    @Value("${vprofiles}")
    private String vprofile;

    public void execute() throws Exception {

        /** 1. 구글 시트에서 목표 비중 가져오기 */
        List<List<Object>> sheetDataList = sheetDataImportService.getSheetsData();
        List<SheetDto> sheetList = parseSheetData(sheetDataList);

        /** 2. 잔고 조회 */
        OverseasStockBalanceResponseDto sbrDto= getCurrentBalance();
        if(!StockBalanceResponseCheck(sbrDto)) return;
        List<OverseasBalanceOutput1Dto> rawHoldingStocks = sbrDto.getOutput1();             // 보유 중인 종목 조회.
        long cashBalance = Parser.safeParseLong(getCurrentCashBalance().getFrcrOrdPsblAmt1());    // 보유 중인 예수금 조회.

        log.warn("[WARN]실예수금 총액: {}", cashBalance);

        /// FOR TEST ///
        if(vprofile.equals("dev")){
            cashBalance = 1000;
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



        /** 3. 보유하고 있지 않은 종목이 있다면, 해당 종목을 먼저 구매함.(단, 보유 중인 종목은 지정된 비율만큼 이미 보유하고 있다고 가정.) */
        // 3-1. 미보유 종목 추출.( 미보유 종목이더라도 목표비중이 0이라면 제외함. )
        List<OverseasStockDto> tempHoldingStocks = holdingStocks;
        List<SheetDto> unholdingStockList = sheetList.stream()
                .filter(sheet -> sheet.getTargetRatio() > 0)
                .filter(sheet -> tempHoldingStocks.stream()
                        .noneMatch(own -> own.getSymb().equals(sheet.getStockCode())))
                .toList();

        // 미보유 종목이 존재한다면,
        if (!unholdingStockList.isEmpty()) {
            // 3-1. 전체 금액 계산.
            // 포트폴리오 총액 = 보유 종목 평가금액 합 + 예수금
            long portfolioTotal = getPortfolioTotal(holdingStocks,  cashBalance);

            // 3-2. 미보유 종목 매수 필요 리스트 추출.
            List<OverseasStockDto> toBuyList = calculateUnholdingBuys(unholdingStockList, portfolioTotal, cashBalance, stockInfoMap);

            if (!toBuyList.isEmpty()) {

                // 3-3. 추가 매수가 필요한 종목 주문.
                orderStocks(toBuyList);

                // 3-4. 잔존 예상 예수금 계산.
                cashBalance = computeEstimateRestCashBalance(cashBalance, toBuyList);
                log.warn("[WARN]미보유 종목 매수 후 예상 예수금 총액: {}", cashBalance);

                // 3-4. 미보유 종목이 매수되었는지 확인.
                // 5초 간격으로 10번 확인.
                boolean buyCompleted = waitForBuyCompletion(toBuyList, 10, 5000);

                if ( !buyCompleted ) {
                    log.warn("[WARN]미보유 종목 매수 미체결 상태로 리밸런싱 시작.");
                }
            }
        }

        /** 4. 이후에 보유 종목에 대해 포트폴리오 비율과 비교하여 추가 매수 진행.(보유 종목의 현재 비율은 내림 처리.) */
        // 4-1. 잔고 재조회.
        sbrDto = getCurrentBalance();
        if(!StockBalanceResponseCheck(sbrDto)) return;

        rawHoldingStocks = sbrDto.getOutput1();                                        // 미보유 매수 후 현재 보유 중인 종목 재확인.
        holdingStocks = getEnrichedHoldingStocks(rawHoldingStocks, stockInfoMap);

        if(vprofile.equals("prod")) {
            cashBalance = Parser.safeParseLong(getCurrentCashBalance().getFrcrOrdPsblAmt1());   // 미보유 매수 후 남은 실제 예수금 확인.
        }

        // 4-2. 추가 매수 필요 리스트 추출.
        List<OverseasStockDto> rebalanceBuyList = calculateRebalanceBuys(holdingStocks, sheetList, cashBalance);

        if (!rebalanceBuyList.isEmpty()) {
            // 4-3. 추가 매수가 필요한 종목 주문.
            orderStocks(rebalanceBuyList);
            // 4-4. 잔존 예상 예수금 계산.
            cashBalance = computeEstimateRestCashBalance(cashBalance, rebalanceBuyList);
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
            long value = row.size() > 3 ? Parser.safeParseLong(row.get(3)) : 0;;
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
    private long getPortfolioTotal(List<OverseasStockDto> holdingStocks, long cashBalance) {
        return holdingStocks.stream()
                .mapToLong(h -> Parser.safeParseLong(h.getEvluAmt()))
                .sum() + cashBalance;
    }

    /**
     * 미보유 종목 매수 계산.
     * @param unholdingStockList 미보유 종목 목록
     * @param portfolioTotal 총 보유 종목 평가금 + 현금 예수금
     * @return resultList 매수 대상 종목 리스트
     * @throws Exception
     */
    private List<OverseasStockDto> calculateUnholdingBuys(List<SheetDto> unholdingStockList, long portfolioTotal, long cashBalance, Map<String, StockInfoDto> stockInfoMap) throws Exception {

        List<OverseasStockDto> resultList = new ArrayList<>();
        List<Map<String, Object>> resultLogList = new ArrayList<>();

        for (SheetDto p : unholdingStockList) {
            String symbol = p.getStockCode();
            // 티커에 맞는 종목 마스터 파일 정보 세팅.
            StockInfoDto stockInfo = stockInfoMap.get(symbol);

            // 목표 금액 = 포트폴리오 총액 * 목표 비중
            long targetAmount = (long)Math.floor(portfolioTotal * (p.getTargetRatio() / 100.0));

            // 미보유 종목이므로 현재 보유금액은 0
            long currentHoldingAmount = 0L;

            // 부족분 = 목표 금액 - 현재 보유 금액
            long needToBuyAmount = targetAmount - currentHoldingAmount;
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
//                quantityToBuy = needToBuyAmount / stockPrice;
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
        long sumNeedToBuyAmount = resultLogList.stream()
                .mapToLong(m -> (long) m.get("needToBuyAmount"))
                .sum();

        for (Map<String, Object> res : resultLogList) {
            double buyRatio = sumNeedToBuyAmount > 0
                    ? ((long) res.get("needToBuyAmount") * 100.0) / sumNeedToBuyAmount
                    : 0.0;
            res.put("buyRatio", buyRatio);

            log.info("미보유 종목 {}({}): 구매금액 {}원, 실제 구매비율 {}%",
                    res.get("name"),
                    res.get("code"),
                    res.get("needToBuyAmount"),
                    buyRatio
            );
        }

        return resultList;
    }

    /**
     * 미보유 종목 매수 체결 확인.
     * @param toBuyList 매수 필요 종목 리스트
     * @param maxRetries 재시도 횟수
     * @param intervalMillis 재시도 간격
     * @return
     */
    public boolean waitForBuyCompletion(List<OverseasStockDto> toBuyList, int maxRetries, long intervalMillis) {
        ObjectMapper mapper = new ObjectMapper();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // 현재 잔고 조회
                OverseasStockBalanceResponseDto sbrDto = getCurrentBalance();
                if(!StockBalanceResponseCheck(sbrDto)) return false;

                List<OverseasBalanceOutput1Dto> holdingStocks = sbrDto.getOutput1();

                // 모든 toBuyList 종목이 원하는 수량 이상 보유하고 있는지 확인.
                boolean isAllBought = toBuyList.stream().allMatch(toBuy -> {
                    OverseasBalanceOutput1Dto matched = holdingStocks.stream()
                            .filter(h -> h.getOvrsPdno().equals(toBuy.getSymb()))
                            .findFirst()
                            .orElse(null);

                    if (matched == null) return false;

                    int holdingQty = Integer.parseInt(matched.getOrdPsblQty()); // 현재 매도 가능 수량
                    int expectedQty = Integer.parseInt(toBuy.getOrdQty());   // 매수 요청 수량
                    return holdingQty >= expectedQty;
                });

                if (isAllBought) {
                    log.warn("[INFO] 모든 매수 체결 완료 확인됨. 다음 프로세스로 진행.");
                    return true;
                }

                log.info("[INFO] 매수 체결 대기 중... (시도 {}/{})", attempt, maxRetries);
                Thread.sleep(intervalMillis);

            } catch (Exception e) {
                log.error("[ERROR] 매수 체결 확인 중 예외 발생", e);
                return false;
            }
        }
        log.error("[ERROR] 지정된 시간 내 매수 체결 확인 실패");
        return false;
    }


    /**
     * 보유 종목 중 추가 매수 필요 종목 계산.
     * @param holdingStocks 현재 보유 종목 목록
     * @param sheetList 포트폴리오 목표 비중 목록
     * @param cashBalance 현재 예수금
     * @return resultList 매수 대상 종목 리스트
     * @throws Exception
     */
    private List<OverseasStockDto> calculateRebalanceBuys(List<OverseasStockDto> holdingStocks, List<SheetDto> sheetList, long cashBalance) throws Exception {
        // 1. 총 평가 금액 계산. (보유 종목 평가금 + 예수금)
        long totalEvalAmount = getPortfolioTotal(holdingStocks, cashBalance);

        // 2. 비율 비교 후 부족분 매수.
        List<OverseasStockDto> resultList = new ArrayList<>();

        for (OverseasStockDto holding : holdingStocks) {
            if (cashBalance <= 0) {
                log.info("[INFO]예수금 소진으로 추가 매수 중단");
                break;
            }

            String stockCode = holding.getSymb();
            long evalAmt = Parser.safeParseLong(holding.getEvluAmt());

            // 포트폴리오 목표 비율 추출.
            double targetRatio = sheetList.stream()
                    .filter(s -> s.getStockCode().equals(stockCode))
                    .mapToDouble(SheetDto::getTargetRatio)
                    .findFirst()
                    .orElse(0.0);

            // 현재 비율 계산.
            double currentRatio = (evalAmt / (double) totalEvalAmount) * 100.0;

            // 목표 비중 보다 낮을 때만 매수 진행.
            if (currentRatio < targetRatio) {
                double shortageRatio = targetRatio - currentRatio;                              // 목표비중 - 현재비중
                long shortageAmount = Math.round(totalEvalAmount * (shortageRatio / 100.0));    // 목표 비중에 도달하기 위해 필요한 추가 매수 금액.

                // 잔액 초과 방지.( 추가 매수 필요 금액보다 예수금이 적은 경우 예수금에 맞게 매수되도록 금액 조정. )
                if (shortageAmount > cashBalance) {
                    shortageAmount = cashBalance;
                }

                // 현재가 조회.
                StockPriceResponseDto sprDto = getCurrentStockPrice(stockCode, holding.getOvrsExcgCd());
                long stockPrice = Parser.safeParseLong(sprDto.getLast());

                // 수량 계산. (0주 허용)
                long quantityToBuy = shortageAmount / stockPrice;

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

                    long buyCost = quantityToBuy * stockPrice;
                    cashBalance -= buyCost;

                    log.info("[INFO] 보유 종목 비율 조정 매수: {} ({}) ({}주, {}원), 남은 예수금: {}원",
                            stockCode, holding.getSymbName(), quantityToBuy, buyCost, cashBalance);
                }
            }
        }
        return resultList;
    }

    private void orderStocks(List<OverseasStockDto> orders) throws Exception {
        for (OverseasStockDto order : orders) {

            if (Validator.isValidOverseasOrder(order)) {
                try {
                    overseasStockService.orderOverseasStock(order);
                    log.info("[INFO] Order placed: {}", orders);

                } catch (Exception e) {
                    log.error("[ERROR] Failed to place order for {}: {}", order.getSymb(), e.getMessage());
                }
            } else {
                log.warn("[WARN] Invalid order skipped: {}", order);
            }
        }
    }

    private long computeEstimateRestCashBalance(long cashBalance, List<OverseasStockDto> orders) throws Exception {
        return cashBalance - orders.stream().mapToLong(dto -> {
            try {
                long price = Long.parseLong(dto.getOvrsOrdUnpr()); // 매수 단가
                long qty = Long.parseLong(dto.getOrdQty());    // 매수 수량
                return price * qty;
            } catch (NumberFormatException e) {
                // 숫자 변환 실패 시 0 처리
                return 0L;
            }
        }).sum();
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
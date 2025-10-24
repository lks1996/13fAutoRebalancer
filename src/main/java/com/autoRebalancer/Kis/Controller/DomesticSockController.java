package com.autoRebalancer.Kis.Controller;

import com.autoRebalancer.Kis.Dto.StockDto;
import com.autoRebalancer.Kis.Service.DomesticStockService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/domesticStock")
public class DomesticSockController {

    private final DomesticStockService domesticStockService;

    @Autowired
    public DomesticSockController(DomesticStockService domesticStockService) {
        this.domesticStockService = domesticStockService;
    }

    /**
     * 국내주식 잔고 조회
     */
    @GetMapping("/balance")
    public void getDomesticStockBalance(StockDto stockDto) {
        domesticStockService.getBalance(stockDto);
    }

    /**
     * 국내주식현재가 시세
     */
    @GetMapping("/inquirePrice")
    public void getDomesticStockPrice(StockDto orderStock) {
        domesticStockService.getDomesticStockPrice(orderStock);
    }

    /**
     * 국내주식주문(현금)
     */
    @GetMapping("/order")
    public void orderDomesticStockCash(StockDto orderStock) throws JsonProcessingException {
        domesticStockService.orderDomesticStockCash(orderStock);
    }

    /**
     * 주식일별주문체결조회
     */
    @GetMapping("/dailyCcld")
    public void getDomesticDailyCcld(StockDto stockDto) throws JsonProcessingException {
        domesticStockService.getDomesticDailyCcld(stockDto);
    }

    /**
     * 모의투자 전용 거래내역 조회
     */
    @GetMapping("/getTestOrderHistory")
    public void getTestDomesticOrderHistory(StockDto stockDto) throws JsonProcessingException {
        domesticStockService.getTestDomesticOrderHistory(stockDto);
    }
}

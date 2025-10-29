package com.autoRebalancer.Kis.Controller;

import com.autoRebalancer.Kis.Dto.OverseasStockDto;
import com.autoRebalancer.Kis.Service.OverseasStockService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/overseasStock")
public class OverseasStockController {

    private final OverseasStockService overseasStockService;

    @Autowired
    public OverseasStockController(OverseasStockService overseasStockService) {
        this.overseasStockService = overseasStockService;
    }

    /**
     * 해외주식잔고조회
     */
    @GetMapping("/balance")
    public void getOverseasStockBalance(OverseasStockDto stockDto) {
        overseasStockService.getBalance(stockDto);
    }

    /**
     * 해외주식현재가 시세
     */
    @GetMapping("/inquirePrice")
    public void getOverseasStockPrice(OverseasStockDto orderStock) {
        overseasStockService.getOverseasStockPrice(orderStock);
    }

    /**
     * 해외주식주문
     */
    @GetMapping("/order")
    public void orderOverseasStock(OverseasStockDto orderStock) throws JsonProcessingException {
        overseasStockService.orderOverseasStock(orderStock);
    }

    /**
     * 해외주식 주문체결내역
     */
    @GetMapping("/dailyCcld")
    public void getOverseasCcld(OverseasStockDto stockDto) throws JsonProcessingException {
        overseasStockService.getOverseasCcnl(stockDto);
    }
}

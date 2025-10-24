package com.autoRebalancer.Kis.Controller;

import com.autoRebalancer.Kis.Service.AutoTradeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trade")
public class AutoTradeController {

    private final AutoTradeService autoTradeService;

    public AutoTradeController(AutoTradeService autoTradeService) {
        this.autoTradeService = autoTradeService;
    }

    @GetMapping("/run")
    public String runNow() throws Exception {
        autoTradeService.execute();
        return "자동 트레이드 실행 완료";
    }
}

package com.autoRebalancer.Scheduler;

import com.autoRebalancer.Kis.Service.AutoTradeService;
import com.autoRebalancer._13f.Service.FilingProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RebalancingScheduler {

    private final AutoTradeService autoTradeService;

    // 생성자를 통해 필요한 서비스를 주입받습니다.
    public RebalancingScheduler(AutoTradeService autoTradeService) {
        this.autoTradeService = autoTradeService;
    }

    /**
     * 매주 일요일 새벽 4시에 실행.
     * cron = "[초] [분] [시] [일] [월] [요일]"
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduleRebalancing() throws Exception {
        log.info("===== [START] Scheduled Rebalancing Job =====");
        try {
            autoTradeService.execute();
            log.info("===== [SUCCESS] Scheduled Rebalancing Job Finished =====");
        } catch (Exception e) {
            log.error("===== [FAIL] An error occurred during the scheduled Rebalancing job =====", e);
        }
    }

}

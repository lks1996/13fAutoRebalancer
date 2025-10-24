package com.autoRebalancer.Scheduler;

import com.autoRebalancer._13f.Service.FilingProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FilingDataScheduler {

    private final FilingProcessService filingProcessService;

    // 생성자를 통해 필요한 서비스를 주입받습니다.
    public FilingDataScheduler(FilingProcessService filingProcessService) {
        this.filingProcessService = filingProcessService;
    }

    /**
     * 매주 일요일 새벽 4시에 실행.
     * cron = "[초] [분] [시] [일] [월] [요일]"
     */
    @Scheduled(cron = "0 0 0 * * SUN,WED")
    public void scheduleWeeklyFilingUpdate() {
        log.info("===== [START] Scheduled Weekly Filing Data Update Job =====");
        try {
            filingProcessService.processLatestFilings();
            log.info("===== [SUCCESS] Scheduled Weekly Filing Data Update Job Finished =====");
        } catch (Exception e) {
            log.error("===== [FAIL] An error occurred during the scheduled job =====", e);
        }
    }

}

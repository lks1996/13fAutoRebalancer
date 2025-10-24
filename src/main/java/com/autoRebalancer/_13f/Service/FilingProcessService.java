package com.autoRebalancer._13f.Service;

import com.autoRebalancer._13f.Dto.Filing;
import com.autoRebalancer._13f.Dto.Holding;
import com.autoRebalancer._13f.Dto.PortfolioHolding;
import com.autoRebalancer._13f.Entity.FilingEntity;
import com.autoRebalancer._13f.Entity.HoldingEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class FilingProcessService {

    // final 키워드를 사용하여 불변성을 보장하고, 생성자 주입을 사용합니다.
    private final DataScrapService scrapService;
    private final FilingPersistenceService persistenceService;

    public FilingProcessService(DataScrapService scrapService, FilingPersistenceService persistenceService) {
        this.scrapService = scrapService;
        this.persistenceService = persistenceService;
    }

    /**
     * 특정 기간에 대한 최신 13F Filing 데이터 조회.
     */
    @Transactional
    public void processLatestFilings() throws IOException, InterruptedException {

        log.warn("[START] Processing for processLatestFilings");

        // 1. [조회] 특정 기간의 Filing 리스트 API 요청.
        List<Filing> filingDtos = scrapService.getFilings();

        // 2. [저장] Filing 리스트 DB 저장.
        List<FilingEntity> savedFilings = persistenceService.saveFilings(filingDtos);

        log.warn("[SUCCESS] Processing finished for processLatestFilings");

    }

    /**
     * 특정 CIK에 대한 최신 13F 공시 데이터 조회.
     * @param cik 처리할 기관의 CIK 번호
     */
    @Transactional
    public List<PortfolioHolding> getOrFetchHoldingsByCik(String cik) throws IOException, InterruptedException {
        log.info("[START] Processing for CIK: {}", cik);

        // 1. DB에서 cik 기관의 가장 최신 Filing 데이터 조회.
        FilingEntity latestFiling = persistenceService.getLatestFilingByCik(cik);
        String accessionNumber = latestFiling.getAccessionNumber();

        // // 2. Filing의 accession number로 저장된 Holding 데이터가 DB에 있는지 확인.
        List<HoldingEntity> rawHoldings = persistenceService.getHoldingsByAccessionNumber(accessionNumber);

        // 3. 없으면 api 호출해서 데이터 받아와서 DB에 저장.
        if (rawHoldings.isEmpty()) {
            log.info("Holdings not found in DB. Fetching from API for {}", accessionNumber);
            List<Holding> holdingDtos = scrapService.getHoldings(latestFiling.getCik(), accessionNumber); // 원본 DTO 받아옴
            persistenceService.saveHoldings(latestFiling, holdingDtos); // 원본 저장
        } else {
            log.info("Raw holdings already exist in DB for {}.", accessionNumber);
        }

        // 4. 최종적으로 DB에서 필터링/병합된 결과를 조회하여 반환
        log.info("◀◀◀ [SUCCESS] Fetching final aggregated data for CIK: {}", cik);
        return persistenceService.getFilteredAndAggregatedHoldings(accessionNumber);
    }
}

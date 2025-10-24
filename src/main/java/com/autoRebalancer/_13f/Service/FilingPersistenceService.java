package com.autoRebalancer._13f.Service;

import com.autoRebalancer.Common.FilingNotFoundException;
import com.autoRebalancer._13f.Dto.Filer;
import com.autoRebalancer._13f.Dto.Filing;
import com.autoRebalancer._13f.Dto.Holding;
import com.autoRebalancer._13f.Dto.PortfolioHolding;
import com.autoRebalancer._13f.Entity.FilingEntity;
import com.autoRebalancer._13f.Entity.HoldingEntity;
import com.autoRebalancer.Common.Mapper.FilingMapper;
import com.autoRebalancer.Common.Mapper.HoldingMapper;
import com.autoRebalancer._13f.Repository.FilingRepository;
import com.autoRebalancer._13f.Repository.HoldingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class FilingPersistenceService {
    private final FilingRepository filingRepository;
    private final HoldingRepository holdingRepository;
    private final FilingMapper filingMapper;
    private final HoldingMapper holdingMapper;

    @Autowired
    public FilingPersistenceService(FilingRepository filingRepository
            , HoldingRepository holdingRepository
            , FilingMapper filingMapper
            , HoldingMapper holdingMapper) {
        this.filingRepository = filingRepository;
        this.holdingRepository = holdingRepository;
        this.filingMapper = filingMapper;
        this.holdingMapper = holdingMapper;
    }

    /**
     * DB에 저장된 모든 기관 목록 조회.
     * @return Filer 리스트
     */
    public List<Filer> findAllFilers() {
        log.info("Finding all distinct filers from DB.");
        return filingRepository.findDistinctFilers();
    }

    /**
     * 저장된 Filing 리스트 중 특정 cik를 가진 기관의 최신 Filing 조회.
     * @param cik 조회할 filing의 기관 고유번호
     * @return 조회된 HoldingEntity 리스트
     */
    public FilingEntity getLatestFilingByCik(String cik) {
        log.info("Finding the latest filing from DB for CIK: {}", cik);
        return filingRepository.findFirstByCikOrderByPeriodOfReportDescFiledAsOfDateDesc(cik)
                .orElseThrow(() -> new FilingNotFoundException("No filing data found in the database for CIK: " + cik));
    }

    /**
     * Filing DTO 리스트 DB에 저장.
     * @param filingDtos 저장할 Filing DTO 리스트
     */
    public List<FilingEntity> saveFilings(List<Filing> filingDtos) {
        List<FilingEntity> filteredFilings = new ArrayList<>();

        // 1. filingDtos -> filingEntityList 변환.
        List<FilingEntity> filingEntities = filingMapper.toEntityList(filingDtos);

        // 2. 신규 데이터만 저장하도록 필터링.
        for(FilingEntity filing : filingEntities){
            if (filingRepository.existsByAccessionNumber(filing.getAccessionNumber())) {
                log.info("Filing with Accession Number {} already exists. Skipping save.", filing.getAccessionNumber());
                continue;
            }
            filteredFilings.add(filing);
        }

        // 3. DB 저장.
        List<FilingEntity> savedFilings = filingRepository.saveAll(filteredFilings);
        log.warn("Successfully saved filings {} ", filteredFilings.size());

        // 4. 저장한 데이터 리턴.
        return savedFilings;
    }

    /**
     * Accession Number로 저장된 Holding 리스트를 조회.
     * @param accessionNumber 조회할 공시의 고유번호
     * @return 조회된 HoldingEntity 리스트
     */
    public List<HoldingEntity> getHoldingsByAccessionNumber(String accessionNumber) {
        log.info("Fetching existing holdings for accession number: {}", accessionNumber);
        return holdingRepository.findByFilingAccessionNumber(accessionNumber);
    }

    /**
     * DB에서 특정 공시의 보유 종목을 조회,
     * 요청된 조건(보통주, 비중 0.1% 이상)에 맞게 병합하고 필터링 후 반환.
     */
    public List<PortfolioHolding> getFilteredAndAggregatedHoldings(String accessionNumber) {
        log.info("Aggregating and filtering holdings from DB for accession number: {}", accessionNumber);
        return holdingRepository.findAndAggregateHoldings(accessionNumber);
    }

    /**
     * Holding DTO 리스트를 받아 부모 FilingEntity와 관계를 맺고 DB에 저장.
     * @param parentFiling 부모 FilingEntity
     * @param holdingDtos 저장할 Holding DTO 리스트
     */
    public List<HoldingEntity> saveHoldings(FilingEntity parentFiling, List<Holding> holdingDtos) {
        if (holdingDtos == null || holdingDtos.isEmpty()) {
            log.info("No holdings to save for filing {}", parentFiling.getAccessionNumber());
            return Collections.emptyList();
        }

        long totalPortfolioValue = parentFiling.getTableValueTotal() != null && parentFiling.getTableValueTotal() > 0
                ? parentFiling.getTableValueTotal()
                : 1L;

        // 1. holdingDtos -> HoldingEntityList 변환.
        List<HoldingEntity> holdingEntities = holdingDtos.stream().map(dto -> {
            HoldingEntity entity = holdingMapper.toEntity(dto);
            entity.setFiling(parentFiling); // 부모 관계 설정

            // 2. 개별 비중을 미리 계산하여 저장
            double percentage = ((double) entity.getValue() / totalPortfolioValue) * 100.0;
            double roundedPercentage = Math.round(percentage * 10000.0) / 10000.0; // 소수점 4자리까지 정밀도 확보
            entity.setPortfolioPercentage(roundedPercentage);

            return entity;
        }).collect(Collectors.toList());

        // 3. DB 저장.
        List<HoldingEntity> savedHoldings = holdingRepository.saveAll(holdingEntities);
        log.info("Successfully saved holdings {} ", holdingEntities.size());

        // 4. 저장한 데이터 리턴.
        return savedHoldings;
    }
}

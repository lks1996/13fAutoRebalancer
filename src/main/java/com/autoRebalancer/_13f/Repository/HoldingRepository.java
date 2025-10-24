package com.autoRebalancer._13f.Repository;

import com.autoRebalancer._13f.Dto.PortfolioHolding;
import com.autoRebalancer._13f.Entity.HoldingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoldingRepository extends JpaRepository<HoldingEntity, Long> {

    boolean existsByAccessionNumber(String accessionNumber);

    List<HoldingEntity> findByFilingAccessionNumber(String accessionNumber);

    /**
     * 특정 공시에 대해 보통주 필터링, 발행사 병합, 비중 필터링 후,
     * 최종 결과를 PortfolioHolding DTO 리스트 반환.
     * @param accessionNumber
     * @return
     */
    @Query("SELECT new com.autoRebalancer._13f.Dto.PortfolioHolding(" +
            "   h.ticker, " +
            "   h.nameOfIssuer, " +
            "   SUM(h.shares), " + // Entity 필드명 사용
            "   SUM(h.value), " +
            "   SUM(h.portfolioPercentage)) " +
            "FROM HoldingEntity h " +
            "WHERE h.filing.accessionNumber = :accessionNumber " +
            "AND h.titleOfClass = 'COM' " +
            "GROUP BY h.ticker, h.nameOfIssuer " +
            "HAVING SUM(h.portfolioPercentage) >= 0.1 " + // 비중 0.1% 이상 필터링
            "ORDER BY SUM(h.value) DESC")
    List<PortfolioHolding> findAndAggregateHoldings(@Param("accessionNumber") String accessionNumber);
}

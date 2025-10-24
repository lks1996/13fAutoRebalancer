package com.autoRebalancer._13f.Repository;

import com.autoRebalancer._13f.Dto.Filer;
import com.autoRebalancer._13f.Entity.FilingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FilingRepository extends JpaRepository<FilingEntity, String> {

    boolean existsByAccessionNumber(String accessionNumber);

    @Query("SELECT DISTINCT new com.autoRebalancer._13f.Dto.Filer(f.cik, f.companyName) FROM FilingEntity f ORDER BY f.companyName ASC")
    List<Filer> findDistinctFilers();

    Optional<FilingEntity> findFirstByCikOrderByPeriodOfReportDescFiledAsOfDateDesc(String cik);
}

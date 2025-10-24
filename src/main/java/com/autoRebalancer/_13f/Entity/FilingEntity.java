package com.autoRebalancer._13f.Entity;

import com.autoRebalancer.Common.BooleanToYNConverter;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "FILINGS")
@Setter
@Getter
@NoArgsConstructor
public class FilingEntity {

    @Id
    @Column(name = "ACCESSION_NUMBER", length = 20, nullable = false)
    private String accessionNumber;

    @Column(name = "URL", length = 512)
    private String url;

    @Column(name = "SUBMISSION_TYPE", length = 20)
    private String submissionType;

    @Column(name = "PUBLIC_DOCUMENT_COUNT")
    private Integer publicDocumentCount;

    @Column(name = "PERIOD_OF_REPORT")
    private LocalDate periodOfReport;

    @Column(name = "FILED_AS_OF_DATE")
    private LocalDate filedAsOfDate;

    @Column(name = "DATE_AS_OF_CHANGE")
    private LocalDate dateAsOfChange;

    @Column(name = "EFFECTIVENESS_DATE")
    private LocalDate effectivenessDate;

    @Column(name = "CIK", length = 10, nullable = false)
    private String cik;

    @Column(name = "COMPANY_NAME", length = 255)
    private String companyName;

    @Column(name = "IRS_NUMBER", length = 20)
    private String irsNumber;

    @Column(name = "STATE_OF_INCORPORATION", length = 2)
    private String stateOfIncorporation;

    @Column(name = "FISCAL_YEAR_END", length = 10)
    private String fiscalYearEnd;

    @Column(name = "FORM_TYPE", length = 20)
    private String formType;

    @Column(name = "SEC_ACT", length = 20)
    private String secAct;

    @Column(name = "SEC_FILE_NUMBER", length = 20)
    private String secFileNumber;

    @Column(name = "FILM_NUMBER", length = 20)
    private String filmNumber;

    @Column(name = "BUSINESS_ADDRESS", length = 255)
    private String businessAddress;

    @Column(name = "BUSINESS_PHONE", length = 50)
    private String businessPhone;

    @Column(name = "TABLE_VALUE_TOTAL")
    private Long tableValueTotal;

    @Column(name = "TABLE_ENTRY_TOTAL")
    private Long tableEntryTotal;

    @Column(name = "IS_AMENDMENT", length = 1)
    @Convert(converter = BooleanToYNConverter.class)
    private boolean isAmendment;

    @Column(name = "AMENDMENT_TYPE", length = 50)
    private String amendmentType;

    @Column(name = "CONF_DENIED_EXPIRED", length = 1)
    @Convert(converter = BooleanToYNConverter.class)
    private boolean confDeniedExpired;

    @Column(name = "CONF_DATE_DENIED_EXPIRED")
    private LocalDate confDateDeniedExpired;

    @Column(name = "AMENDMENT_DATE_REPORTED")
    private LocalDate amendmentDateReported;

    // Filing과 Holding의 1:N 관계
    @OneToMany(mappedBy = "filing", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<HoldingEntity> holdings = new ArrayList<>();

}

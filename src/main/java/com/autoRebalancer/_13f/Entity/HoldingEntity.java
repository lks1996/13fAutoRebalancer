package com.autoRebalancer._13f.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "HOLDINGS")
@Setter
@Getter
@NoArgsConstructor
public class HoldingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HOLDING_ID")
    private Long holdingId;

    @Column(name = "ACCESSION_NUMBER", insertable = false, updatable = false)
    private String accessionNumber;

    @Column(name = "CIK")
    private String cik;

    @Column(name = "NAME_OF_ISSUER", length = 255)
    private String nameOfIssuer;

    @Column(name = "TITLE_OF_CLASS", length = 50)
    private String titleOfClass;

    @Column(name = "CUSIP", length = 9)
    private String cusip;

    @Column(name = "TICKER", length = 20)
    private String ticker;

    @Column(name = "VALUE")
    private Long value;

    @Column(name = "SHARES")
    private Long shares;

    @Column(name = "SSH_PRNAMT_TYPE", length = 10)
    private String sshPrnamtType;

    @Column(name = "INVESTMENT_DISCRETION", length = 20)
    private String investmentDiscretion;

    @Column(name = "VOTING_AUTHORITY_SOLE")
    private Long votingAuthoritySole;

    @Column(name = "VOTING_AUTHORITY_SHARED")
    private Long votingAuthorityShared;

    @Column(name = "VOTING_AUTHORITY_NONE")
    private Long votingAuthorityNone;

    @Column(name = "PUT_CALL", length = 10)
    private String putCall;

    @Column(name = "PORTFOLIO_PERCENTAGE")
    private Double portfolioPercentage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCESSION_NUMBER", nullable = false)
    @JsonBackReference
    private FilingEntity filing;
}

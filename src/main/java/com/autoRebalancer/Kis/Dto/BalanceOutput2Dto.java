package com.autoRebalancer.Kis.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceOutput2Dto {

    @JsonProperty("dnca_tot_amt")
    private String dncaTotAmt;

    @JsonProperty("nxdy_excc_amt")
    private String nxdyExccAmt;

    @JsonProperty("prvs_rcdl_excc_amt")
    private String prvsRcdlExccAmt;

    @JsonProperty("cma_evlu_amt")
    private String cmaEvluAmt;

    @JsonProperty("bfdy_buy_amt")
    private String bfdyBuyAmt;

    @JsonProperty("thdt_buy_amt")
    private String thdtBuyAmt;

    @JsonProperty("nxdy_auto_rdpt_amt")
    private String nxdyAutoRdptAmt;

    @JsonProperty("bfdy_sll_amt")
    private String bfdySllAmt;

    @JsonProperty("thdt_sll_amt")
    private String thdtSllAmt;

    @JsonProperty("d2_auto_rdpt_amt")
    private String d2AutoRdptAmt;

    @JsonProperty("bfdy_tlex_amt")
    private String bfdyTlexAmt;

    @JsonProperty("thdt_tlex_amt")
    private String thdtTlexAmt;

    @JsonProperty("tot_loan_amt")
    private String totLoanAmt;

    @JsonProperty("scts_evlu_amt")
    private String sctsEvluAmt;

    @JsonProperty("tot_evlu_amt")
    private String totEvluAmt;

    @JsonProperty("nass_amt")
    private String nassAmt;

    @JsonProperty("fncg_gld_auto_rdpt_yn")
    private String fncgGldAutoRdptYn;

    @JsonProperty("pchs_amt_smtl_amt")
    private String pchsAmtSmtlAmt;

    @JsonProperty("evlu_amt_smtl_amt")
    private String evluAmtSmtlAmt;

    @JsonProperty("evlu_pfls_smtl_amt")
    private String evluPflsSmtlAmt;

    @JsonProperty("tot_stln_slng_chgs")
    private String totStlnSlngChgs;

    @JsonProperty("bfdy_tot_asst_evlu_amt")
    private String bfdyTotAsstEvluAmt;

    @JsonProperty("asst_icdc_amt")
    private String asstIcdcAmt;

    @JsonProperty("asst_icdc_erng_rt")
    private String asstIcdcErngRt;

}

package com.autoRebalancer.Kis.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceOutput1Dto {

    // 종목코드(6자리)
    @JsonProperty("pdno")
    private String pdno;

    // 종목이름
    @JsonProperty("prdt_name")
    private String prdtName;

    @JsonProperty("trad_dvsn_name")
    private String tradDvsnName;

    @JsonProperty("bfdy_buy_qty")
    private String bfdyBuyQty;

    @JsonProperty("bfdy_sll_qty")
    private String bfdySllQty;

    @JsonProperty("thdt_buyqty")
    private String thdtBuyQty;

    @JsonProperty("thdt_sll_qty")
    private String thdtSllQty;

    @JsonProperty("hldg_qty")
    private String hldgQty;

    @JsonProperty("ord_psbl_qty")
    private String ordPsblQty;

    @JsonProperty("pchs_avg_pric")
    private String pchsAvgPric;

    @JsonProperty("pchs_amt")
    private String pchsAmt;

    @JsonProperty("prpr")
    private String prpr;

    @JsonProperty("evlu_amt")
    private String evluAmt;

    @JsonProperty("evlu_pfls_amt")
    private String evluPflsAmt;

    @JsonProperty("evlu_pfls_rt")
    private String evluPflsRt;

    @JsonProperty("evlu_erng_rt")
    private String evluErngRt;

    @JsonProperty("loan_dt")
    private String loanDt;

    @JsonProperty("loan_amt")
    private String loanAmt;

    @JsonProperty("stln_slng_chgs")
    private String stlnSlngChgs;

    @JsonProperty("expd_dt")
    private String expdDt;

    @JsonProperty("fltt_rt")
    private String flttRt;

    @JsonProperty("bfdy_cprs_icdc")
    private String bfdyCprsIcdc;

    @JsonProperty("item_mgna_rt_name")
    private String itemMgnaRtName;

    @JsonProperty("grta_rt_name")
    private String grtaRtName;

    @JsonProperty("sbst_pric")
    private String sbstPric;

    @JsonProperty("stck_loan_unpr")
    private String stckLoanUnpr;
}

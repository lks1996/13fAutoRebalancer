package com.autoRebalancer.Kis.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OverseasBalanceOutput1Dto {

    @JsonProperty("cano")
    private String cano; // 종합계좌번호 (앞 8자리)

    @JsonProperty("acnt_prdt_cd")
    private String acntPrdtCd; // 계좌상품코드

    @JsonProperty("prdt_type_cd")
    private String prdtTypeCd; // 상품유형코드

    @JsonProperty("ovrs_pdno")
    private String ovrsPdno; // 해외상품번호 (티커/심볼)

    @JsonProperty("ovrs_item_name")
    private String ovrsItemName; // 해외종목명

    @JsonProperty("frcr_evlu_pfls_amt")
    private String frcrEvluPflsAmt; // 외화평가손익금액

    @JsonProperty("evlu_pfls_rt")
    private String evluPflsRt; // 평가손익율

    @JsonProperty("pchs_avg_pric")
    private String pchsAvgPric; // 매입평균가격

    @JsonProperty("ovrs_cblc_qty")
    private String ovrsCblcQty; // 해외잔고수량 (보유 수량)

    @JsonProperty("ord_psbl_qty")
    private String ordPsblQty; // 주문가능수량 (매도 가능 수량)

    @JsonProperty("frcr_pchs_amt1")
    private String frcrPchsAmt1; // 외화매입금액

    @JsonProperty("ovrs_stck_evlu_amt")
    private String ovrsStckEvluAmt; // 해외주식평가금액

    @JsonProperty("now_pric2")
    private String nowPric2; // 현재가격2

    @JsonProperty("tr_crcy_cd")
    private String trCrcyCd; // 거래통화코드 (USD, HKD 등)

    @JsonProperty("ovrs_excg_cd")
    private String ovrsExcgCd; // 해외거래소코드 (NASD, NYSE 등)

    @JsonProperty("loan_type_cd")
    private String loanTypeCd; // 대출유형코드

    @JsonProperty("loan_dt")
    private String loanDt; // 대출일자

    @JsonProperty("expd_dt")
    private String expdDt; // 만기일자
}

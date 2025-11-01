package com.autoRebalancer.Kis.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OverseasPsblAmountOutputDto {

    @JsonProperty("tr_crcy_cd")
    private String trCrcyCd; // 거래통화코드

    @JsonProperty("ord_psbl_frcr_amt")
    private String ordPsblFrcrAmt; // 주문가능외화금액

    @JsonProperty("sll_ruse_psbl_amt")
    private String sllRusePsblAmt; // 매도재사용가능금액

    @JsonProperty("ovrs_ord_psbl_amt")
    private String ovrsOrdPsblAmt; // 해외주문가능금액 (외화 기준)

    @JsonProperty("max_ord_psbl_qty")
    private String maxOrdPsblQty; // 최대주문가능수량 (외화 기준)

    @JsonProperty("echm_af_ord_psbl_amt")
    private String echmAfOrdPsblAmt; // 환전이후주문가능금액 (사용되지 않음)

    @JsonProperty("echm_af_ord_psbl_qty")
    private String echmAfOrdPsblQty; // 환전이후주문가능수량 (사용되지 않음)

    @JsonProperty("ord_psbl_qty")
    private String ordPsblQty; // 주문가능수량

    @JsonProperty("exrt")
    private String exrt; // 환율

    @JsonProperty("frcr_ord_psbl_amt1")
    private String frcrOrdPsblAmt1; // 외화주문가능금액1 (통합 기준)

    @JsonProperty("ovrs_max_ord_psbl_qty")
    private String ovrsMaxOrdPsblQty; // 해외최대주문가능수량 (통합 기준)
}

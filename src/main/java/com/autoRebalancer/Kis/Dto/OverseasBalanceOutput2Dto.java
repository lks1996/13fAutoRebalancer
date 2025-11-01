package com.autoRebalancer.Kis.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OverseasBalanceOutput2Dto {

    @JsonProperty("frcr_pchs_amt1")
    private String frcrPchsAmt1; // 외화매입금액1

    @JsonProperty("ovrs_rlzt_pfls_amt")
    private String ovrsRlztPflsAmt; // 해외실현손익금액

    @JsonProperty("ovrs_tot_pfls")
    private String ovrsTotPfls; // 해외총손익

    @JsonProperty("rlzt_erng_rt")
    private String rlztErngRt; // 실현수익율

    @JsonProperty("tot_evlu_pfls_amt")
    private String totEvluPflsAmt; // 총평가손익금액

    @JsonProperty("tot_pftrt")
    private String totPftrt; // 총수익률

    @JsonProperty("frcr_buy_amt_smtl1")
    private String frcrBuyAmtSmtl1; // 외화매수금액합계1

    @JsonProperty("ovrs_rlzt_pfls_amt2")
    private String ovrsRlztPflsAmt2; // 해외실현손익금액2

    @JsonProperty("frcr_buy_amt_smtl2")
    private String frcrBuyAmtSmtl2; // 외화매수금액합계2
}

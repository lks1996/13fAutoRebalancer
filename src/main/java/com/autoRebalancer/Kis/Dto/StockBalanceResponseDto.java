package com.autoRebalancer.Kis.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockBalanceResponseDto {

    @JsonProperty("rt_cd")
    private String rtCd;

    @JsonProperty("msg_cd")
    private String msgCd;

    @JsonProperty("msg1")
    private String msg1;

    @JsonProperty("ctx_area_fk100")
    private String ctxAreaFk100;

    @JsonProperty("ctx_area_nk100")
    private String ctxAreaNk100;

    @JsonProperty("output1")
    private List<BalanceOutput1Dto> output1;

    @JsonProperty("output2")
    private List<BalanceOutput2Dto> output2;
}

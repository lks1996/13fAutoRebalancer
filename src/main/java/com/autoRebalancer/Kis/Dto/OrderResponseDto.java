package com.autoRebalancer.Kis.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OrderResponseDto {
    @JsonProperty("rt_cd")
    private String rtCd;    // 결과 코드

    @JsonProperty("msg_cd")
    private String msgCd;   // 메시지 코드

    @JsonProperty("msg1")
    private String msg1;    // 메시지 내용
}

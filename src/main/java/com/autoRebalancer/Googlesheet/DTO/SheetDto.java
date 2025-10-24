package com.autoRebalancer.Googlesheet.DTO;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class SheetDto {

    // 계좌유형
    String accountType;
    // 티커/종목코드
    String stockCode;
    // 종목명
    String stockName;
    // 보유 주식 수
    long sshPrnamt;
    // 가치
    long value;
    // 종목별 희망 비율
    double targetRatio;

    public SheetDto(
            String accountType
            , String stockCode
            , String stockName
            , long sshPrnamt
            , long value
            , double targetRatio
    ) {
        this.accountType = accountType;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.sshPrnamt = sshPrnamt;
        this.value = value;
        this.targetRatio = targetRatio;
    }

}

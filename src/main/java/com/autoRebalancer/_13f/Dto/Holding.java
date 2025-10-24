package com.autoRebalancer._13f.Dto;

import com.google.gson.annotations.SerializedName;

// 13F 공시 내의 개별 보유 종목(Holding) 원본 정보를 담는 record
public record Holding(

        @SerializedName("accession_number")
        String accessionNumber,

        @SerializedName("cik")
        String cik,

        @SerializedName("name_of_issuer")
        String nameOfIssuer,

        @SerializedName("title_of_class")
        String titleOfClass,

        @SerializedName("cusip")
        String cusip,

        @SerializedName("ticker")
        String ticker,

        @SerializedName("value")
        long value,

        @SerializedName("ssh_prnamt")
        long sshPrnamt,

        @SerializedName("ssh_prnamt_type")
        String sshPrnamtType,

        @SerializedName("investment_discretion")
        String investmentDiscretion,

        @SerializedName("voting_authority_sole")
        long votingAuthoritySole,

        @SerializedName("voting_authority_shared")
        long votingAuthorityShared,

        @SerializedName("voting_authority_none")
        long votingAuthorityNone,

        @SerializedName("put_call")
        String putCall
) {}

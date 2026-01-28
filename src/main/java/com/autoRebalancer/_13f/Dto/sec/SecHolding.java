package com.autoRebalancer._13f.Dto.sec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecHolding {

    @JacksonXmlProperty(localName = "nameOfIssuer")
    private String nameOfIssuer;

    @JacksonXmlProperty(localName = "titleOfClass")
    private String titleOfClass;

    @JacksonXmlProperty(localName = "cusip")
    private String cusip;

    @JacksonXmlProperty(localName = "value")
    private long value;

    @JacksonXmlProperty(localName = "shrsOrPrnAmt")
    private ShareAmount shrsOrPrnAmt;

    @JacksonXmlProperty(localName = "investmentDiscretion")
    private String investmentDiscretion;

    @JacksonXmlProperty(localName = "votingAuthority")
    private VotingAuthority votingAuthority;

    @JacksonXmlProperty(localName = "putCall")
    private String putCall;

    // --- Inner Classes for Nested XML Tags ---

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShareAmount {
        @JacksonXmlProperty(localName = "sshPrnamt")
        private long sshPrnamt;

        @JacksonXmlProperty(localName = "sshPrnamtType")
        private String sshPrnamtType;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VotingAuthority {
        @JacksonXmlProperty(localName = "Sole")
        private long sole;

        @JacksonXmlProperty(localName = "Shared")
        private long shared;

        @JacksonXmlProperty(localName = "None")
        private long none;
    }
}
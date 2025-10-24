package com.autoRebalancer._13f.Dto;

import com.google.gson.annotations.SerializedName;

// 최신 공시 정보를 담기 위한 record
public record Filing(

        @SerializedName("url")
        String url,

        @SerializedName("accession_number")
        String accessionNumber,

        @SerializedName("submission_type")
        String submissionType,

        @SerializedName("public_document_count")
        int publicDocumentCount,

        @SerializedName("period_of_report")
        String periodOfReport,

        @SerializedName("filed_as_of_date")
        String filedAsOfDate,

        @SerializedName("date_as_of_change")
        String dateAsOfChange,

        @SerializedName("effectiveness_date")
        String effectivenessDate,

        @SerializedName("cik")
        String cik,

        @SerializedName("company_name")
        String companyName,

        @SerializedName("irs_number")
        String irsNumber,

        @SerializedName("state_of_incorporation")
        String stateOfIncorporation,

        @SerializedName("fiscal_year_end")
        String fiscalYearEnd,

        @SerializedName("form_type")
        String formType,

        @SerializedName("sec_act")
        String secAct,

        @SerializedName("sec_file_number")
        String secFileNumber,

        @SerializedName("film_number")
        String filmNumber,

        @SerializedName("business_address")
        String businessAddress,

        @SerializedName("business_phone")
        String businessPhone,

        @SerializedName("table_value_total")
        long tableValueTotal,

        @SerializedName("table_entry_total")
        long tableEntryTotal,

        @SerializedName("is_amendment")
        boolean isAmendment,

        @SerializedName("amendment_type")
        String amendmentType,

        @SerializedName("conf_denied_expired")
        boolean confDeniedExpired,

        @SerializedName("conf_date_denied_expired")
        String confDateDeniedExpired,

        @SerializedName("amendment_date_reported")
        String amendmentDateReported
) {}


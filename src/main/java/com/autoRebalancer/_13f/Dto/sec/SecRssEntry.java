package com.autoRebalancer._13f.Dto.sec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecRssEntry {
    @JacksonXmlProperty(localName = "title")
    private String title;

    @JacksonXmlProperty(localName = "updated")
    private String updated;

    @JacksonXmlProperty(localName = "link")
    private Link link;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Link {
        @JacksonXmlProperty(isAttribute = true)
        private String href;
    }
}